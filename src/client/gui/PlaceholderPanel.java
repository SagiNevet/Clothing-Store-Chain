package client.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

/**
 * A tab that is not implemented yet, showing what it will contain and in which
 * stage it arrives.
 * <p>
 * The panel exists so that the whole main window - the tabs, the role based
 * view, the header - can be built and demonstrated in stage 3, before the
 * inventory, the customers, the reports and the chat are written. Each of these
 * panels is replaced by the real screen in a later stage.
 * </p>
 */
public class PlaceholderPanel extends JPanel {

    /** Serialization version, required because Swing components are serializable. */
    private static final long serialVersionUID = 1L;

    /** The size of the title shown in the middle of the panel. */
    private static final float TITLE_FONT_SIZE = 20f;

    /**
     * Builds a placeholder tab.
     *
     * @param featureName  the name of the feature this tab will hold
     * @param description  a sentence describing what the finished tab will do
     * @param arrivalStage the stage in which the real screen is written
     */
    public PlaceholderPanel(String featureName, String description, String arrivalStage) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel centeredContent = new JPanel();
        centeredContent.setLayout(new BoxLayout(centeredContent, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(featureName);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel stageLabel = new JLabel("Implemented in " + arrivalStage);
        stageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        stageLabel.setEnabled(false);

        centeredContent.add(Box.createVerticalGlue());
        centeredContent.add(titleLabel);
        centeredContent.add(Box.createVerticalStrut(12));
        centeredContent.add(descriptionLabel);
        centeredContent.add(Box.createVerticalStrut(6));
        centeredContent.add(stageLabel);
        centeredContent.add(Box.createVerticalGlue());

        add(centeredContent, BorderLayout.CENTER);
    }
}
