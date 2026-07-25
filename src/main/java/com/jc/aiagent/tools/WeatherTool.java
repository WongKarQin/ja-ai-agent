package com.jc.aiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 天气查询工具 - 使用高德地图 API
 * 支持：1) 指定城市查询  2) 自动获取用户IP定位查询
 */
@Slf4j
@Component
public class WeatherTool {

    @Value("${weather.api.key:}")
    private String apiKey;

    // 天气查询API
    @Value("${weather.api.url:https://restapi.amap.com/v3/weather/weatherInfo}")
    private String weatherApiUrl;

    // IP定位API
    @Value("${weather.ip.url:https://restapi.amap.com/v3/ip}")
    private String ipLocationUrl;

    // 地理编码API（城市名转adcode）
    @Value("${weather.geo.url:https://restapi.amap.com/v3/geocode/geo}")
    private String geoCodeUrl;

    // 常用城市编码缓存（精确到市/区县级别）
    private static final Map<String, String> CITY_CODE_MAP = new HashMap<>();

    static {
        // 直辖市
        CITY_CODE_MAP.put("北京", "110000");
        CITY_CODE_MAP.put("北京市", "110000");
        CITY_CODE_MAP.put("上海", "310000");
        CITY_CODE_MAP.put("上海市", "310000");
        CITY_CODE_MAP.put("天津", "120000");
        CITY_CODE_MAP.put("天津市", "120000");
        CITY_CODE_MAP.put("重庆", "500000");
        CITY_CODE_MAP.put("重庆市", "500000");

        // 广东省
        CITY_CODE_MAP.put("广州", "440100");
        CITY_CODE_MAP.put("广州市", "440100");
        CITY_CODE_MAP.put("深圳", "440300");
        CITY_CODE_MAP.put("深圳市", "440300");
        CITY_CODE_MAP.put("东莞", "441900");
        CITY_CODE_MAP.put("东莞市", "441900");
        CITY_CODE_MAP.put("佛山", "440600");
        CITY_CODE_MAP.put("佛山市", "440600");
        CITY_CODE_MAP.put("珠海", "440400");
        CITY_CODE_MAP.put("珠海市", "440400");
        CITY_CODE_MAP.put("中山", "442000");
        CITY_CODE_MAP.put("中山市", "442000");
        CITY_CODE_MAP.put("惠州", "441300");
        CITY_CODE_MAP.put("惠州市", "441300");
        CITY_CODE_MAP.put("汕头", "440500");
        CITY_CODE_MAP.put("汕头市", "440500");
        CITY_CODE_MAP.put("江门", "440700");
        CITY_CODE_MAP.put("江门市", "440700");

        // 浙江省
        CITY_CODE_MAP.put("杭州", "330100");
        CITY_CODE_MAP.put("杭州市", "330100");
        CITY_CODE_MAP.put("宁波", "330200");
        CITY_CODE_MAP.put("宁波市", "330200");
        CITY_CODE_MAP.put("温州", "330300");
        CITY_CODE_MAP.put("温州市", "330300");
        CITY_CODE_MAP.put("嘉兴", "330400");
        CITY_CODE_MAP.put("嘉兴市", "330400");
        CITY_CODE_MAP.put("绍兴", "330600");
        CITY_CODE_MAP.put("绍兴市", "330600");

        // 江苏省
        CITY_CODE_MAP.put("南京", "320100");
        CITY_CODE_MAP.put("南京市", "320100");
        CITY_CODE_MAP.put("苏州", "320500");
        CITY_CODE_MAP.put("苏州市", "320500");
        CITY_CODE_MAP.put("无锡", "320200");
        CITY_CODE_MAP.put("无锡市", "320200");
        CITY_CODE_MAP.put("常州", "320400");
        CITY_CODE_MAP.put("常州市", "320400");
        CITY_CODE_MAP.put("徐州", "320300");
        CITY_CODE_MAP.put("徐州市", "320300");
        CITY_CODE_MAP.put("南通", "320600");
        CITY_CODE_MAP.put("南通市", "320600");

        // 四川省
        CITY_CODE_MAP.put("成都", "510100");
        CITY_CODE_MAP.put("成都市", "510100");

        // 湖北省
        CITY_CODE_MAP.put("武汉", "420100");
        CITY_CODE_MAP.put("武汉市", "420100");

        // 陕西省
        CITY_CODE_MAP.put("西安", "610100");
        CITY_CODE_MAP.put("西安市", "610100");

        // 河南省
        CITY_CODE_MAP.put("郑州", "410100");
        CITY_CODE_MAP.put("郑州市", "410100");

        // 湖南省
        CITY_CODE_MAP.put("长沙", "430100");
        CITY_CODE_MAP.put("长沙市", "430100");

        // 辽宁省
        CITY_CODE_MAP.put("沈阳", "210100");
        CITY_CODE_MAP.put("沈阳市", "210100");
        CITY_CODE_MAP.put("大连", "210200");
        CITY_CODE_MAP.put("大连市", "210200");

        // 山东省
        CITY_CODE_MAP.put("青岛", "370200");
        CITY_CODE_MAP.put("青岛市", "370200");
        CITY_CODE_MAP.put("济南", "370100");
        CITY_CODE_MAP.put("济南市", "370100");

        // 福建省
        CITY_CODE_MAP.put("厦门", "350200");
        CITY_CODE_MAP.put("厦门市", "350200");
        CITY_CODE_MAP.put("福州", "350100");
        CITY_CODE_MAP.put("福州市", "350100");
    }

    // ==================== 对外暴露的工具方法 ====================

    /**
     * 【方式1】获取指定城市的当前天气情况
     */
    @Tool(description = "获取指定城市的当前天气情况，包括温度、天气状况、风向风力、湿度等")
    public String getWeather(@ToolParam(description = "城市名称，如：深圳、北京、上海") String city) {
        if (apiKey == null || apiKey.isBlank()) {
            return String.format("%s暂未获取天气数据（API Key未配置）", city);
        }

        try {
            // 1. 获取城市编码（adcode）
            String cityCode = resolveCityCode(city);
            if (cityCode == null) {
                return String.format("未找到城市「%s」的编码信息，无法查询天气", city);
            }

            // 2. 查询天气并返回
            return queryWeatherByAdcode(city, cityCode);
        } catch (Exception e) {
            return String.format("查询%s天气时发生错误：%s", city, e.getMessage());
        }
    }

    /**
     * 【方式2】自动获取用户当前地理位置，并查询当地天气
     * 通过用户IP自动定位城市，无需传入城市参数
     */
    @Tool(description = "自动获取用户当前地理位置，查询用户所在城市的当前天气情况")
    public String getLocalWeather() {
        if (apiKey == null || apiKey.isBlank()) {
            return "暂未获取天气数据（API Key未配置）";
        }

        try {
            // 1. 通过IP获取用户所在城市
            IpLocationResult location = getLocationByIp();
            if (location == null || location.getAdcode() == null) {
                return "无法自动获取您的地理位置，请尝试指定城市名称查询天气";
            }

            // 2. 查询天气并返回
            String cityName = location.getCity();
            String adcode = location.getAdcode();

            // 记录定位信息用于调试
            log.info("[IP定位] 省份: {}, 城市: {}, adcode: {}, 客户端IP: {}",
                    location.getProvince(), cityName, adcode, location.getClientIp());

            return queryWeatherByAdcode(cityName, adcode);
        } catch (Exception e) {
            return String.format("自动获取本地天气时发生错误：%s", e.getMessage());
        }
    }

    // ==================== 内部私有方法 ====================

    /**
     * 根据城市名解析城市编码（adcode）
     * 优先级：本地缓存 -> 地理编码API查询
     */
    private String resolveCityCode(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return null;
        }

        // 1. 先查本地缓存
        String code = CITY_CODE_MAP.get(cityName);
        if (code != null) {
            return code;
        }

        // 2. 尝试规范化匹配（去除"市/县/区"后缀）
        String normalized = cityName.replace("市", "").replace("县", "").replace("区", "");
        code = CITY_CODE_MAP.get(normalized);
        if (code != null) {
            return code;
        }

        // 3. 通过高德地理编码API查询
        return getAdcodeByGeoCode(cityName);
    }

    /**
     * 通过高德地理编码API，将城市名转换为adcode
     * 接口：https://restapi.amap.com/v3/geocode/geo?address=城市名&key=APIKey
     */
    private String getAdcodeByGeoCode(String cityName) {
        try {
            String url = String.format("%s?address=%s&key=%s&output=JSON", geoCodeUrl, cityName, apiKey);
            String response = HttpUtil.get(url, 5000);
            JSONObject json = JSONUtil.parseObj(response);

            // 判断成功：status=1 且 info=OK
            if (!"1".equals(json.getStr("status")) || !"OK".equals(json.getStr("info"))) {
                return null;
            }

            JSONArray geocodes = json.getJSONArray("geocodes");
            if (geocodes == null || geocodes.isEmpty()) {
                return null;
            }

            // 返回第一个结果的adcode
            return geocodes.getJSONObject(0).getStr("adcode");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过IP定位获取用户所在城市信息
     * 接口：https://restapi.amap.com/v3/ip?key=APIKey&ip=可选
     * 若不传ip参数，则自动取HTTP请求中的客户端IP进行定位
     *
     * 注意：IP定位返回的adcode是城市级别的编码
     */
    private IpLocationResult getLocationByIp() {
        try {
            // 获取客户端真实IP（考虑代理情况）
            String clientIp = getClientIp();

            String url;
            if (clientIp != null && !clientIp.isBlank()
                    && !"127.0.0.1".equals(clientIp)
                    && !"0:0:0:0:0:0:0:1".equals(clientIp)) {
                // 传入客户端IP进行定位
                url = String.format("%s?key=%s&ip=%s&output=JSON", ipLocationUrl, apiKey, clientIp);
            } else {
                // 不传IP，高德自动从HTTP请求中获取（适用于生产环境）
                url = String.format("%s?key=%s&output=JSON", ipLocationUrl, apiKey);
            }

            String response = HttpUtil.get(url, 5000);
            JSONObject json = JSONUtil.parseObj(response);

            // 判断成功：status=1 且 info=OK
            if (!"1".equals(json.getStr("status")) || !"OK".equals(json.getStr("info"))) {
                System.err.println("[IP定位失败] status=" + json.getStr("status")
                        + ", info=" + json.getStr("info")
                        + ", infocode=" + json.getStr("infocode"));
                return null;
            }

            // 解析返回数据
            IpLocationResult result = new IpLocationResult();
            result.setProvince(json.getStr("province"));
            result.setCity(json.getStr("city"));
            result.setAdcode(json.getStr("adcode"));
            result.setRectangle(json.getStr("rectangle")); // 经纬度范围
            result.setClientIp(clientIp);

            // 直辖市处理：IP定位返回的city在直辖市情况下就是直辖市名称
            // 如北京返回 province="北京市", city="北京市", adcode="110000"
            // 这是正确的，因为直辖市没有市级行政区划，adcode直接就是市级编码

            return result;
        } catch (Exception e) {
            System.err.println("[IP定位异常] " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据adcode查询天气（核心查询逻辑）
     */
    private String queryWeatherByAdcode(String cityName, String adcode) {
        // 查询天气（extensions=base 返回实况天气）
        String url = String.format("%s?city=%s&key=%s&extensions=base&output=JSON", weatherApiUrl, adcode, apiKey);

        log.info("[天气查询] URL: {}", url);

        String response = HttpUtil.get(url, 5000);
        JSONObject json = JSONUtil.parseObj(response);

        // 判断返回状态：status为1表示成功，infocode为10000表示正确
        if (!"1".equals(json.getStr("status")) || !"10000".equals(json.getStr("infocode"))) {
            String info = json.getStr("info", "未知错误");
            System.err.printf("[天气查询失败] status=%s, infocode=%s, info=%s%n",
                    json.getStr("status"), json.getStr("infocode"), info);
            return String.format("查询%s天气失败：%s", cityName, info);
        }

        // 解析实况天气数据（lives数组）
        JSONArray lives = json.getJSONArray("lives");
        if (lives == null || lives.isEmpty()) {
            return String.format("未获取到%s的实况天气数据", cityName);
        }

        JSONObject now = lives.getJSONObject(0);

        // 打印原始返回数据用于调试
        System.out.printf("[天气数据] province=%s, city=%s, weather=%s, temp=%s°C%n",
                now.getStr("province"), now.getStr("city"),
                now.getStr("weather"), now.getStr("temperature"));

        return String.format(
                "%s当前天气：%s，气温%s°C，%s，风力%s级，湿度%s%%，数据发布时间：%s",
                now.getStr("city", cityName),
                now.getStr("weather"),
                now.getStr("temperature"),
                now.getStr("winddirection"),
                now.getStr("windpower"),
                now.getStr("humidity"),
                now.getStr("reporttime")
        );
    }

    /**
     * 获取客户端真实IP地址（考虑反向代理）
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();

            // 按优先级获取真实IP（考虑Nginx等反向代理）
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            // X-Forwarded-For可能包含多个IP，取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            return ip;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 内部数据类 ====================

    /**
     * IP定位结果封装
     */
    private static class IpLocationResult {
        private String province;
        private String city;
        private String adcode;
        private String rectangle;
        private String clientIp;

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getAdcode() { return adcode; }
        public void setAdcode(String adcode) { this.adcode = adcode; }
        public String getRectangle() { return rectangle; }
        public void setRectangle(String rectangle) { this.rectangle = rectangle; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    }
}