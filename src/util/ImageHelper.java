package util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.ImageIcon;

public class ImageHelper {

    // Centralized Base Paths
    private static final String[] BASE_PATHS = {
            "C:/xampp/htdocs/distrozoneweb/",
            "C:/xampp/htdocs/distrozone/",
            "C:/xampp/htdocs/kasirweb/",
            "C:/xampp/htdocs/kasir/",
            "C:/Users/antartika2/OneDrive/Dokumen/Yanzzzzzz/LSP/distrozoneweb/",
            System.getProperty("user.dir") + "/",
            System.getProperty("user.dir") + "/src/",
            System.getProperty("user.dir") + "/build/classes/"
    };

    /**
     * Resolves the absolute path of an image given a relative path or filename.
     * Checks multiple base directories (Web, Local, etc.)
     */
    public static String resolveImagePath(String path) {
        if (path == null || path.isEmpty())
            return null;

        // Clean the path
        path = path.trim();
        while (path.startsWith("../") || path.startsWith("..\\")) {
            path = path.substring(3);
        }

        File f = new File(path);
        if (f.isAbsolute() && f.exists())
            return path;

        // Check local project paths (priority)
        String[] localPaths = {
                "src/img/" + path,
                "img/" + path,
                "build/classes/img/" + path,
                System.getProperty("user.dir") + File.separator + "img" + File.separator + path
        };

        for (String localPath : localPaths) {
            File localFile = new File(localPath);
            if (localFile.exists())
                return localFile.getAbsolutePath();
        }

        // Clean path for web-style paths
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        cleanPath = cleanPath.replace("/", File.separator).replace("\\", File.separator);

        // Check web base paths
        for (String basePath : BASE_PATHS) {
            String[] webPaths = {
                    basePath + cleanPath,
                    basePath + "assets/uploads/products/" + cleanPath,
                    basePath + "uploads/products/" + cleanPath,
                    basePath + "assets/img/" + cleanPath
            };

            for (String webPath : webPaths) {
                String normalizedPath = webPath.replace("/", File.separator).replace("\\\\", File.separator);
                // Fix double separators
                normalizedPath = normalizedPath.replace(File.separator + File.separator, File.separator);

                File webFile = new File(normalizedPath);
                if (webFile.exists()) {
                    return normalizedPath;
                }
            }
        }

        System.err.println("❌ Image not found: " + path + " (Tried multiple locations)");
        return null; // Not found
    }

    /**
     * Loads an ImageIcon safely, resizing it to the specified width and height.
     * Returns a placeholder if not found.
     */
    public static ImageIcon loadImage(String path, int width, int height) {
        if (path != null && !path.isEmpty()) {
            String resolved = resolveImagePath(path);
            if (resolved != null) {
                try {
                    Image img = new ImageIcon(resolved).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(img);
                } catch (Exception e) {
                    System.err.println("❌ Failed to load image: " + resolved);
                }
            } else {
                System.err.println("❌ Could not resolve path for: " + path);
            }
        }
        return createPlaceholderIcon(width, height);
    }

    public static ImageIcon createPlaceholderIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(230, 230, 230));
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.GRAY);
        g2d.drawRect(0, 0, width - 1, height - 1);
        g2d.drawString("No Img", 5, height / 2);
        g2d.dispose();
        return new ImageIcon(image);
    }
}
