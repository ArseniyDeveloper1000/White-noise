package mainCode.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;



public class imageCompress {
    
    public static byte[] compress(BufferedImage image) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException(
                "Нельзя сжать изображение: изображение равно null"
            );
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            boolean written = ImageIO.write(image, "jpg", baos);

            if (!written) {
                throw new IOException(
                    "Не удалось сжать изображение: JPEG-формат недоступен"
                );
            }

            return baos.toByteArray();
        } finally {
            baos.close();
        }
    }

    
}