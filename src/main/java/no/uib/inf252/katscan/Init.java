package no.uib.inf252.katscan;

import com.bulenkov.darcula.DarculaLaf;
import java.util.Map;
import java.util.Set;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import no.uib.inf252.katscan.view.MainFrame;
import no.uib.inf252.katscan.view.SplashScreen;

/**
 *
 * @author Marcelo Lima
 */
public class Init {
    
    private static MainFrame frameReference;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            UIManager.setLookAndFeel(new DarculaLaf());
                
//            final UIDefaults lookAndFeelDefaults = UIManager.getLookAndFeelDefaults();
//            Set<Map.Entry<Object, Object>> entrySet = lookAndFeelDefaults.entrySet();
//            for (Map.Entry<Object, Object> entry : entrySet) {
//                if (entry.getValue() instanceof Color) {
//                    Color color = (Color) entry.getValue();
//                    lookAndFeelDefaults.put(entry.getKey(), new Color(color.getRed(), color.getBlue(), color.getGreen()));
//                }
//                System.out.println(entry.getKey() + " :: " + entry.getValue());
//            }
//            System.exit(0);
        } catch (UnsupportedLookAndFeelException e) {
            java.util.logging.Logger.getLogger(SplashScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
            try {
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (ClassNotFoundException ex) {
                java.util.logging.Logger.getLogger(SplashScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                java.util.logging.Logger.getLogger(SplashScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(SplashScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            } catch (javax.swing.UnsupportedLookAndFeelException ex) {
                java.util.logging.Logger.getLogger(SplashScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                SplashScreen dialog = new SplashScreen();
                dialog.setVisible(true);
                
                frameReference = new MainFrame();
                frameReference.setVisible(true);
            }
        });
    }

    public static MainFrame getFrameReference() {
        return frameReference;
    }
}
