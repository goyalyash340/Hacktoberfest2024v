import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ImageColorDetection extends JPanel {

	private BufferedImage image;
	private String imagePath;

	public ImageColorDetection(String imagePath) {
		this.imagePath = imagePath;
		try {
			image = ImageIO.read(new File(imagePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			g.drawImage(image, 0, 0, this);
		}
	}

	private Color detectColorsInRegion(int startX, int endX, int startY, int endY) {
		if (image == null) {
			return null;
		}

		Map<Color, Integer> colorCount = new HashMap<>();
		for (int y = startY; y < endY; y++) {
			for (int x = startX; x < endX; x++) {
				int pixel = image.getRGB(x, y);
				Color color = new Color(pixel, true);
				colorCount.put(color, colorCount.getOrDefault(color, 0) + 1);
			}
		}

		Color dominantColor = null;
		int maxCount = 0;
		for (Map.Entry<Color, Integer> entry : colorCount.entrySet()) {
			if (entry.getValue() > maxCount) {
				dominantColor = entry.getKey();
				maxCount = entry.getValue();
			}
		}

		return dominantColor;
	}

	private static boolean isWhite(Color color) {
		int threshold = 235; // Adjust this value if needed
		return color.getRed() >= threshold && color.getGreen() >= threshold && color.getBlue() >= threshold;
	}

	private boolean containsNonWhiteOrBlackInTop10Percent() {
		if (image == null) {
			return false;
		}

		int width = image.getWidth();
		int height = image.getHeight();

		//Changes in Top part of image 10 to 3 to 0.5%
		for (int y = 0; y < height * 0.005; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = image.getRGB(x, y);
				Color color = new Color(pixel, true);

				if (!isWhite(color)) {
					return true;
				}
			}
		}
		return false;
	}

	public void copyToNewFolder(String newFolderPath, String imageName) {
		File sourceFile = new File(imagePath);
		File destFile = new File(newFolderPath + File.separator + imageName);
		try {
			Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Image copied to: " + destFile.getAbsolutePath() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void processImagesInFolder(String folderPath, String newFolderPath) {
		File folder = new File(folderPath);
		File[] listOfFiles = folder
				.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpeg"));

		if (listOfFiles != null) {
			for (File file : listOfFiles) {
				if (file.isFile()) {
					System.out.println("Processing: " + file.getAbsolutePath());
					ImageColorDetection panel = new ImageColorDetection(file.getAbsolutePath());

					if (panel.containsNonWhiteOrBlackInTop10Percent()) {
						System.out.println("Image rejected due to presence of non-white/black color in top 0.5%.\n");
						continue;
					}

					int width = panel.image.getWidth();
					int height = panel.image.getHeight();
					int halfHeight = (int) (height * 0.5);
					int leftWidth = (int) (width * 0.15);
					int rightWidth = (int) (width * 0.85);

					Color leftColor = panel.detectColorsInRegion(0, leftWidth, 0, halfHeight);
					Color rightColor = panel.detectColorsInRegion(rightWidth, width, 0, halfHeight);

					if (isWhite(leftColor) && isWhite(rightColor)) {
						System.out.println("Detected dominant color is white in both left and right.");
						panel.copyToNewFolder(newFolderPath, file.getName());
					} else {
						System.out.println("Image rejected due to non-white dominant color on left or right.\n");
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		String folderPath = "sourceFolderPath";
		String newFolderPath = "destinationFolderPath";

		processImagesInFolder(folderPath, newFolderPath);
	}
}
