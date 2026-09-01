package client.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

public class PlaceholderPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final float TITLE_FONT_SIZE = 20f;

    public PlaceholderPanel(String featureName, String description) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel centeredContent = new JPanel();
        centeredContent.setLayout(new BoxLayout(centeredContent, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(featureName);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centeredContent.add(Box.createVerticalGlue());
        centeredContent.add(titleLabel);
        centeredContent.add(Box.createVerticalStrut(12));
        centeredContent.add(descriptionLabel);
        centeredContent.add(Box.createVerticalGlue());

        add(centeredContent, BorderLayout.CENTER);
    }
}
