package com.jc.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.jc.aiagent.constant.FileConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class PDFGenerationTool {

    private static final Logger log = LoggerFactory.getLogger(PDFGenerationTool.class);

    private PdfFont chineseFont;
    private PdfFont emojiFont;

    private static final float FONT_SIZE = 12f;

    // Emoji 图片缓存：key=emoji字符串, value=PNG字节数组
    private final Map<String, byte[]> emojiImageCache = new HashMap<>();

    // Twemoji CDN 基础URL
    private static final String TWEMOJI_CDN_BASE = "https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/";
    /**
     * 若无法联网，那么采用离线方案：
     * # 下载 Twemoji 完整 PNG 包
     * git clone --depth 1 https://github.com/jdecked/twemoji.git
     * # 使用 assets/72x72/ 目录下的 PNG 文件
     * 然后修改代码中的 TWEMOJI_CDN_BASE 为本地路径：private static final String TWEMOJI_CDN_BASE = "file:///C:/twemoji/assets/72x72/";
     */
    // 本地缓存目录
    private static final String EMOJI_CACHE_DIR = System.getProperty("java.io.tmpdir") + "/twemoji-cache/";

    private void initFonts() throws IOException {
        URL chineseFontUrl = getClass().getResource("/fonts/SIMHEI.ttf");
        if (chineseFontUrl == null) {
            throw new IOException("SIMHEI.ttf not found");
        }
        chineseFont = PdfFontFactory.createFont(
                chineseFontUrl.getPath(),
                PdfEncodings.IDENTITY_H,
                EmbeddingStrategy.PREFER_EMBEDDED);

        // 加载黑白 Emoji 字体作为回退
        try {
            URL emojiFontUrl = getClass().getResource("/fonts/NotoEmoji-Regular.ttf");
            if (emojiFontUrl != null) {
                emojiFont = PdfFontFactory.createFont(
                        emojiFontUrl.getPath(),
                        PdfEncodings.IDENTITY_H,
                        EmbeddingStrategy.PREFER_EMBEDDED);
            } else {
                emojiFont = chineseFont;
            }
        } catch (Exception e) {
            emojiFont = chineseFont;
        }

        // 创建本地缓存目录
        FileUtil.mkdir(EMOJI_CACHE_DIR);
    }

    /**
     * 判断是否是 Emoji
     */
    private boolean isEmoji(int codePoint) {
        if (codePoint == 0x200D || codePoint == 0xFE0F || codePoint == 0xFE0E ||
                (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF)) {
            return true;
        }

        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.EMOTICONS
                || block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS
                || block == Character.UnicodeBlock.DINGBATS
                || block == Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS
                || block == Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
                || block == Character.UnicodeBlock.SYMBOLS_AND_PICTOGRAPHS_EXTENDED_A
                || block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS
                || block == Character.UnicodeBlock.ENCLOSED_ALPHANUMERIC_SUPPLEMENT
                || block == Character.UnicodeBlock.ENCLOSED_IDEOGRAPHIC_SUPPLEMENT
                || block == Character.UnicodeBlock.MAHJONG_TILES
                || block == Character.UnicodeBlock.DOMINO_TILES
                || block == Character.UnicodeBlock.PLAYING_CARDS
                || block == Character.UnicodeBlock.GEOMETRIC_SHAPES_EXTENDED
                || (codePoint >= 0x1F000 && codePoint <= 0x1FFFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x2702 && codePoint <= 0x27B0)
                || (codePoint >= 0x1F900 && codePoint <= 0x1F9FF);
    }

    /**
     * 将 Emoji 转换为 Twemoji 的 codepoint 格式
     * 例如: 🌌 -> 1f30c, 👨‍👩‍👧‍👦 -> 1f468-200d-1f469-200d-1f467-200d-1f466
     */
    private String emojiToTwemojiCode(String emojiText) {
        StringBuilder codeBuilder = new StringBuilder();
        int[] codePoints = emojiText.codePoints().toArray();

        for (int i = 0; i < codePoints.length; i++) {
            int cp = codePoints[i];
            // 跳过变体选择器 FE0E/FE0F（Twemoji 不需要）
            if (cp == 0xFE0E || cp == 0xFE0F) {
                continue;
            }
            if (codeBuilder.length() > 0) {
                codeBuilder.append("-");
            }
            codeBuilder.append(String.format("%04x", cp));
        }

        return codeBuilder.toString().toLowerCase();
    }

    /**
     * 从 Twemoji CDN 下载 Emoji PNG
     */
    private byte[] downloadEmojiFromTwemoji(String emojiText) {
        String emojiCode = emojiToTwemojiCode(emojiText);
        String cacheFileName = EMOJI_CACHE_DIR + emojiCode + ".png";
        Path cachePath = Paths.get(cacheFileName);

        try {
            // 先检查本地缓存
            if (Files.exists(cachePath)) {
                log.debug("Loading emoji from local cache: {}", emojiCode);
                return Files.readAllBytes(cachePath);
            }

            // 从 CDN 下载
            String urlStr = TWEMOJI_CDN_BASE + emojiCode + ".png";
            log.debug("Downloading emoji from CDN: {}", urlStr);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.warn("Failed to download emoji {}: HTTP {}", emojiCode, responseCode);
                return null;
            }

            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
                byte[] imageBytes = out.toByteArray();

                // 保存到本地缓存
                Files.write(cachePath, imageBytes);
                log.debug("Emoji cached: {} ({} bytes)", emojiCode, imageBytes.length);

                return imageBytes;
            }

        } catch (Exception e) {
            log.warn("Failed to get emoji {}: {}", emojiCode, e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Emoji 图片字节（带缓存）
     */
    private byte[] getEmojiImage(String emojiText) {
        // 检查内存缓存
        if (emojiImageCache.containsKey(emojiText)) {
            return emojiImageCache.get(emojiText);
        }

        byte[] imageBytes = downloadEmojiFromTwemoji(emojiText);

        if (imageBytes != null) {
            emojiImageCache.put(emojiText, imageBytes);
        }

        return imageBytes;
    }

    @Tool(description = "Generate a PDF file with given content", returnDirect = true)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {

        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;

        try {
            FileUtil.mkdir(fileDir);
            initFonts();

            try (Document document = new Document(new PdfDocument(new PdfWriter(filePath)))) {
                Paragraph paragraph = new Paragraph();
                paragraph.setTextAlignment(TextAlignment.LEFT);

                int i = 0;
                int emojiCount = 0;
                int emojiImageCount = 0;

                while (i < content.length()) {
                    int codePoint = content.codePointAt(i);
                    int charCount = Character.charCount(codePoint);
                    String ch = new String(Character.toChars(codePoint));

                    // 收集连续相同类型的字符
                    StringBuilder sb = new StringBuilder(ch);
                    i += charCount;
                    boolean currentIsEmoji = isEmoji(codePoint);

                    while (i < content.length()) {
                        int nextCp = content.codePointAt(i);
                        int nextCharCount = Character.charCount(nextCp);
                        boolean nextIsEmoji = isEmoji(nextCp);

                        // 变体选择器、ZWJ、肤色修饰符跟随前一个字符
                        if (nextCp == 0xFE0F || nextCp == 0x200D || nextCp == 0xFE0E ||
                                (nextCp >= 0x1F3FB && nextCp <= 0x1F3FF)) {
                            sb.append(new String(Character.toChars(nextCp)));
                            i += nextCharCount;

                            if (nextCp == 0x200D && i < content.length()) {
                                int zwjNextCp = content.codePointAt(i);
                                int zwjNextCount = Character.charCount(zwjNextCp);
                                sb.append(new String(Character.toChars(zwjNextCp)));
                                i += zwjNextCount;
                            }
                            continue;
                        }

                        if (nextIsEmoji == currentIsEmoji) {
                            sb.append(new String(Character.toChars(nextCp)));
                            i += nextCharCount;
                        } else {
                            break;
                        }
                    }

                    String textStr = sb.toString();

                    if (currentIsEmoji) {
                        emojiCount++;
                        byte[] imageBytes = getEmojiImage(textStr);

                        if (imageBytes != null) {
                            emojiImageCount++;
                            ImageData imageData = ImageDataFactory.create(imageBytes);
                            Image emojiImage = new Image(imageData);

                            // 设置图片大小为字体大小
                            emojiImage.setHeight(FONT_SIZE);
                            // 宽度按比例缩放
                            float aspectRatio = (float) imageData.getWidth() / imageData.getHeight();
                            emojiImage.setWidth(FONT_SIZE * aspectRatio);

                            paragraph.add(emojiImage);
                        } else {
                            // CDN 下载失败，回退到黑白字体
                            log.warn("Using font fallback for emoji: {}", textStr);
                            Text text = new Text(textStr)
                                    .setFont(emojiFont)
                                    .setFontSize(FONT_SIZE);
                            paragraph.add(text);
                        }
                    } else {
                        Text text = new Text(textStr)
                                .setFont(chineseFont)
                                .setFontSize(FONT_SIZE);
                        paragraph.add(text);
                    }
                }

                log.info("PDF generation stats - Total emojis: {}, Rendered as images: {}",
                        emojiCount, emojiImageCount);
                document.add(paragraph);
            }

            return "PDF generated successfully to: " + filePath;
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            return "Error generating PDF: " + e.getMessage();
        }
    }
}