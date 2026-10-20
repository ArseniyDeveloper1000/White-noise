package mainCode.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class imageDecompress {

    public static BufferedImage decompress(byte[] compressedBytes) throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
        BufferedImage img = ImageIO.read(bais);
        return img;
       
        
        
    }
}
