package mainCode.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;



public class imageCompress {


    public static byte[] compress(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] compressedBytes = baos.toByteArray();
        return compressedBytes;
    }
    
}