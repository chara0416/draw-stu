package draw;

import javax.swing.SwingUtilities;

public class Main {
	    public static void main(String[] args) {
	    	// 이벤트 디스패치 스레드에서 GUI 실행
	        SwingUtilities.invokeLater(() -> new MainFrame());
	    }
	
}
