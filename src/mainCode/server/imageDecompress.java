package mainCode.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class imageDecompress {
    
    public static BufferedImage decompress(byte[] compressedBytes)
            throws IOException {

        if (compressedBytes == null) {
            throw new IllegalArgumentException(
                "Нельзя декодировать изображение: массив равен null"
            );
        }

        if (compressedBytes.length == 0) {
            throw new IllegalArgumentException(
                "Нельзя декодировать изображение: массив пуст"
            );
        }

        ByteArrayInputStream bais =
                new ByteArrayInputStream(compressedBytes);

        try {
            BufferedImage img = ImageIO.read(bais);

            if (img == null) {
                throw new IOException(
                    "Полученные данные не являются корректным изображением"
                );
            }

            return img;
        } finally {
            bais.close();
        }
    }
}
