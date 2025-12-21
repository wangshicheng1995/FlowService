package com.flowservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * 图片压缩工具类
 * 用于在发送给 AI API 之前压缩图片，确保不超过大小限制
 */
@Slf4j
public class ImageCompressor {

    /**
     * 目标最大字节数（6MB，Base64 后约 8MB，留 2MB 余量）
     */
    private static final long TARGET_MAX_SIZE = 6 * 1024 * 1024;

    /**
     * 图片最大边长（像素），保持足够分辨率用于食物识别
     */
    private static final int MAX_DIMENSION = 2048;

    /**
     * 初始 JPEG 质量（0.85 = 85%，视觉上几乎无损）
     */
    private static final float INITIAL_QUALITY = 0.85f;

    /**
     * 最低 JPEG 质量（不低于 60%，保证基本识别效果）
     */
    private static final float MIN_QUALITY = 0.60f;

    /**
     * 质量递减步长
     */
    private static final float QUALITY_STEP = 0.05f;

    /**
     * 压缩图片
     * 策略：先缩放到合理尺寸，再进行质量压缩
     *
     * @param file 上传的图片文件
     * @return 压缩后的图片字节数组
     * @throws IOException 如果图片处理失败
     */
    public static byte[] compress(MultipartFile file) throws IOException {
        byte[] originalBytes = file.getBytes();
        long originalSize = originalBytes.length;

        log.info("开始图片压缩: fileName={}, originalSize={}KB",
                file.getOriginalFilename(), originalSize / 1024);

        // 如果原图已经足够小，直接返回
        if (originalSize <= TARGET_MAX_SIZE) {
            log.info("图片大小已在限制内，无需压缩");
            return originalBytes;
        }

        // 读取图片
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (originalImage == null) {
            log.warn("无法读取图片格式，返回原图");
            return originalBytes;
        }

        // 步骤1：缩放图片（如果需要）
        BufferedImage scaledImage = scaleImage(originalImage);

        // 步骤2：渐进式质量压缩
        byte[] compressedBytes = progressiveCompress(scaledImage);

        log.info("图片压缩完成: originalSize={}KB -> compressedSize={}KB, 压缩率={:.1f}%",
                originalSize / 1024,
                compressedBytes.length / 1024,
                (1 - (double) compressedBytes.length / originalSize) * 100);

        return compressedBytes;
    }

    /**
     * 按比例缩放图片，确保最大边不超过限制
     */
    private static BufferedImage scaleImage(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // 计算缩放比例
        double scale = 1.0;
        if (originalWidth > MAX_DIMENSION || originalHeight > MAX_DIMENSION) {
            scale = Math.min(
                    (double) MAX_DIMENSION / originalWidth,
                    (double) MAX_DIMENSION / originalHeight);
        }

        // 如果不需要缩放，直接返回原图
        if (scale >= 1.0) {
            log.info("图片尺寸已在限制内: {}x{}", originalWidth, originalHeight);
            return original;
        }

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        log.info("缩放图片: {}x{} -> {}x{} (scale={:.2f})",
                originalWidth, originalHeight, newWidth, newHeight, scale);

        // 使用高质量的缩放算法
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // 设置高质量渲染
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 填充白色背景（处理透明 PNG）
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, newWidth, newHeight);

        // 绘制缩放后的图片
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaledImage;
    }

    /**
     * 渐进式质量压缩
     * 从高质量开始，逐步降低直到满足大小要求
     */
    private static byte[] progressiveCompress(BufferedImage image) throws IOException {
        float quality = INITIAL_QUALITY;
        byte[] result = null;

        while (quality >= MIN_QUALITY) {
            result = compressWithQuality(image, quality);

            if (result.length <= TARGET_MAX_SIZE) {
                log.info("压缩成功: quality={:.0f}%, size={}KB", quality * 100, result.length / 1024);
                return result;
            }

            log.info("当前压缩后仍过大: quality={:.0f}%, size={}KB, 继续降低质量...",
                    quality * 100, result.length / 1024);
            quality -= QUALITY_STEP;
        }

        // 如果最低质量仍然太大，返回最后一次压缩的结果
        log.warn("已达最低质量限制，返回当前压缩结果: size={}KB", result.length / 1024);
        return result;
    }

    /**
     * 以指定质量压缩图片为 JPEG 格式
     */
    private static byte[] compressWithQuality(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("未找到 JPEG ImageWriter");
        }

        ImageWriter writer = writers.next();
        try {
            ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.write(null, new IIOImage(image, null, null), param);

            ios.close();
        } finally {
            writer.dispose();
        }

        return outputStream.toByteArray();
    }
}
