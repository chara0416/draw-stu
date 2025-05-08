package draw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainFrame extends JFrame {
    // 그림 그리는 패널
    private DrawingPanel drawingPanel;
    // 도구 선택하는 패널
    private JPanel toolPanel;
    // 색상 선택하는 패널
    private JPanel colorPanel;
    // 상태 표시하는 패널 (현재 어떤 도구 선택했는지)
    private JPanel statusPanel;
    
    // 도구 버튼들
    private JToggleButton pencilButton; // 연필 버튼
    private JToggleButton lineButton;   // 직선 버튼
    private JToggleButton rectButton;   // 사각형 버튼
    private JToggleButton ovalButton;   // 원 버튼
    private JToggleButton eraserButton; // 지우개 버튼
    private JToggleButton textButton;   // 텍스트 버튼
    
    // 색상 버튼과 모두 지우기 버튼
    private JButton colorButton;  // 현재 선택된 색상 표시
    private JButton clearButton;  // 모두 지우기 버튼
    
    // 선 두께 조절
    private JSlider strokeSlider; // 선 두께 슬라이더
    private JLabel strokeLabel;   // 선 두께 라벨
    private JLabel statusLabel;   // 상태 표시 라벨
    
    // 현재 상태 변수들
    private Color currentColor = Color.BLACK; // 현재 색상
    private int currentStroke = 3;            // 현재 선 두께
    
    // 도구 버튼들을 그룹으로 묶어서 하나만 선택되게 함
    private ButtonGroup toolGroup;
    
    // 창 크기 설정
    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 600;
    
    // 그리기 도구 종류
    public enum DrawingTool {
        PENCIL, LINE, RECTANGLE, OVAL, ERASER, TEXT
    }
    
    // 현재 선택된 도구 (처음에는 연필)
    private DrawingTool currentTool = DrawingTool.PENCIL;
    
    // 생성자 - 프로그램이 시작될 때 호출됨
    public MainFrame() {
        // 창 제목 설정
        setTitle("내가 만든 그림판");
        // 창 크기 설정
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        // 창 닫기 버튼 눌렀을 때 프로그램 종료
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 레이아웃 설정 (동서남북중앙 배치)
        setLayout(new BorderLayout());
        
        // 각 부분 생성
        createMenuBar();    // 메뉴바 만들기
        createToolPanel();  // 도구 패널 만들기
        createColorPanel(); // 색상 패널 만들기
        createDrawingPanel(); // 그림 그리는 패널 만들기
        createStatusPanel(); // 상태 표시 패널 만들기
        
        // 각 패널 배치하기
        add(toolPanel, BorderLayout.WEST);     // 도구 패널은 왼쪽에
        add(colorPanel, BorderLayout.EAST);    // 색상 패널은 오른쪽에
        add(drawingPanel, BorderLayout.CENTER); // 그림 패널은 중앙에
        add(statusPanel, BorderLayout.SOUTH);  // 상태 패널은 아래에
        
        // 창 보이게 하기
        setVisible(true);
    }
    
    // 메뉴바 만드는 메소드
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 파일 메뉴 만들기
        JMenu fileMenu = new JMenu("파일");
        JMenuItem newMenuItem = new JMenuItem("새로 만들기");
        JMenuItem openMenuItem = new JMenuItem("열기");
        JMenuItem saveMenuItem = new JMenuItem("저장");
        JMenuItem exitMenuItem = new JMenuItem("종료");
        
        // 각 메뉴 항목에 기능 추가
        newMenuItem.addActionListener(e -> clearDrawing());   // 새로 만들기 - 모두 지우기
        openMenuItem.addActionListener(e -> openImage());     // 열기 - 이미지 불러오기
        saveMenuItem.addActionListener(e -> saveImage());     // 저장 - 이미지 저장하기
        exitMenuItem.addActionListener(e -> System.exit(0));  // 종료 - 프로그램 종료
        
        // 파일 메뉴에 항목 추가
        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.addSeparator();  // 구분선 추가
        fileMenu.add(exitMenuItem);
        
        // 편집 메뉴 만들기
        JMenu editMenu = new JMenu("편집");
        JMenuItem undoMenuItem = new JMenuItem("실행 취소");
        JMenuItem redoMenuItem = new JMenuItem("다시 실행");
        
        // 실행 취소, 다시 실행 기능 추가
        undoMenuItem.addActionListener(e -> drawingPanel.undo());
        redoMenuItem.addActionListener(e -> drawingPanel.redo());
        
        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);
        
        // 도움말 메뉴 만들기
        JMenu helpMenu = new JMenu("도움말");
        JMenuItem aboutMenuItem = new JMenuItem("정보");
        
        // 프로그램 정보 보여주기
        aboutMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "내가 만든 그림판 프로그램\n버전 1.0\n제작: 준성",
                "정보", JOptionPane.INFORMATION_MESSAGE));
        
        helpMenu.add(aboutMenuItem);
        
        // 메뉴바에 메뉴 추가
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        
        // 프레임에 메뉴바 설정
        setJMenuBar(menuBar);
    }
    
    // 도구 패널 만드는 메소드
    private void createToolPanel() {
        toolPanel = new JPanel();
        // 그리드 레이아웃으로 버튼들 배치 (8행 1열, 간격 5픽셀)
        toolPanel.setLayout(new GridLayout(8, 1, 5, 5));
        toolPanel.setBorder(BorderFactory.createTitledBorder("도구"));
        
        // 버튼 그룹 만들기 (하나만 선택되게)
        toolGroup = new ButtonGroup();
        
        // 각 도구 버튼 만들기
        pencilButton = createToolButton("연필", DrawingTool.PENCIL);
        lineButton = createToolButton("직선", DrawingTool.LINE);
        rectButton = createToolButton("사각형", DrawingTool.RECTANGLE);
        ovalButton = createToolButton("원", DrawingTool.OVAL);
        eraserButton = createToolButton("지우개", DrawingTool.ERASER);
        textButton = createToolButton("텍스트", DrawingTool.TEXT);
        
        // 모두 지우기 버튼
        clearButton = new JButton("모두 지우기");
        clearButton.addActionListener(e -> clearDrawing());
        
        // 선 두께 조절
        strokeLabel = new JLabel("선 두께: " + currentStroke);
        strokeSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, currentStroke);
        strokeSlider.setMajorTickSpacing(5); // 큰 눈금 간격
        strokeSlider.setMinorTickSpacing(1); // 작은 눈금 간격
        strokeSlider.setPaintTicks(true);    // 눈금 표시
        
        // 슬라이더 값이 변할 때 선 두께 변경
        strokeSlider.addChangeListener(e -> {
            currentStroke = strokeSlider.getValue();
            strokeLabel.setText("선 두께: " + currentStroke);
            drawingPanel.setStrokeSize(currentStroke);
        });
        
        // 패널에 컴포넌트 추가
        toolPanel.add(pencilButton);
        toolPanel.add(lineButton);
        toolPanel.add(rectButton);
        toolPanel.add(ovalButton);
        toolPanel.add(eraserButton);
        toolPanel.add(textButton);
        toolPanel.add(clearButton);
        toolPanel.add(strokeLabel);
        toolPanel.add(strokeSlider);
        
        // 처음에는 연필 선택
        pencilButton.setSelected(true);
    }
    
    // 도구 버튼 만드는 메소드
    private JToggleButton createToolButton(String text, DrawingTool tool) {
        JToggleButton button = new JToggleButton(text);
        // 버튼 클릭 시 현재 도구 변경
        button.addActionListener(e -> {
            currentTool = tool;
            drawingPanel.setTool(tool);
            updateStatusBar();
        });
        // 버튼 그룹에 추가 (하나만 선택되게)
        toolGroup.add(button);
        return button;
    }
    
    // 색상 패널 만드는 메소드
    private void createColorPanel() {
        colorPanel = new JPanel();
        colorPanel.setLayout(new GridLayout(10, 1, 5, 5));
        colorPanel.setBorder(BorderFactory.createTitledBorder("색상"));
        
        // 현재 선택된 색상 표시 버튼
        colorButton = new JButton();
        colorButton.setBackground(currentColor);
        colorButton.setPreferredSize(new Dimension(50, 50));
        // 색상 버튼 클릭 시 색상 선택 대화상자 표시
        colorButton.addActionListener(e -> chooseColor());
        
        // 기본 색상 버튼들
        JButton blackButton = createColorButton(Color.BLACK);     // 검정
        JButton redButton = createColorButton(Color.RED);         // 빨강
        JButton greenButton = createColorButton(Color.GREEN);     // 초록
        JButton blueButton = createColorButton(Color.BLUE);       // 파랑
        JButton yellowButton = createColorButton(Color.YELLOW);   // 노랑
        JButton cyanButton = createColorButton(Color.CYAN);       // 하늘색
        JButton magentaButton = createColorButton(Color.MAGENTA); // 분홍
        JButton orangeButton = createColorButton(Color.ORANGE);   // 주황
        JButton grayButton = createColorButton(Color.GRAY);       // 회색
        
        // 패널에 버튼 추가
        colorPanel.add(colorButton);
        colorPanel.add(blackButton);
        colorPanel.add(redButton);
        colorPanel.add(greenButton);
        colorPanel.add(blueButton);
        colorPanel.add(yellowButton);
        colorPanel.add(cyanButton);
        colorPanel.add(magentaButton);
        colorPanel.add(orangeButton);
        colorPanel.add(grayButton);
    }
    
    // 색상 버튼 만드는 메소드
    private JButton createColorButton(Color color) {
        JButton button = new JButton();
        button.setBackground(color);
        button.setPreferredSize(new Dimension(30, 30));
        // 버튼 클릭 시 현재 색상 변경
        button.addActionListener(e -> {
            currentColor = color;
            colorButton.setBackground(color);
            drawingPanel.setColor(color);
        });
        return button;
    }
    
    // 그림 패널 만드는 메소드
    private void createDrawingPanel() {
        drawingPanel = new DrawingPanel();
        drawingPanel.setTool(currentTool);
        drawingPanel.setColor(currentColor);
        drawingPanel.setStrokeSize(currentStroke);
    }
    
    // 상태 패널 만드는 메소드
    private void createStatusPanel() {
        statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        statusLabel = new JLabel("현재 도구: 연필 | 선 두께: " + currentStroke);
        statusPanel.add(statusLabel);
        
        updateStatusBar();
    }
    
    // 상태 표시줄 업데이트 메소드
    private void updateStatusBar() {
        String toolName = "";
        switch (currentTool) {
            case PENCIL: toolName = "연필"; break;
            case LINE: toolName = "직선"; break;
            case RECTANGLE: toolName = "사각형"; break;
            case OVAL: toolName = "원"; break;
            case ERASER: toolName = "지우개"; break;
            case TEXT: toolName = "텍스트"; break;
        }
        
        statusLabel.setText("현재 도구: " + toolName + " | 선 두께: " + currentStroke);
    }
    
    // 색상 선택 대화상자 표시 메소드
    private void chooseColor() {
        Color selectedColor = JColorChooser.showDialog(this, "색상 선택", currentColor);
        if (selectedColor != null) {
            currentColor = selectedColor;
            colorButton.setBackground(currentColor);
            drawingPanel.setColor(currentColor);
        }
    }
    
    // 그림 모두 지우기 메소드
    private void clearDrawing() {
        // 정말 지울건지 물어보기
        int response = JOptionPane.showConfirmDialog(this,
                "현재 그림을 모두 지우시겠습니까?", "확인",
                JOptionPane.YES_NO_OPTION);
        
        if (response == JOptionPane.YES_OPTION) {
            drawingPanel.clear();
        }
    }
    
    // 이미지 저장하는 메소드
    private void saveImage() {
        // 파일 선택 대화상자 만들기
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("이미지 저장");
        // 파일 형식 필터 설정
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG 이미지", "png"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("JPEG 이미지", "jpg", "jpeg"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("BMP 이미지", "bmp"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            // 선택한 파일
            File fileToSave = fileChooser.getSelectedFile();
            String fileName = fileToSave.getAbsolutePath();
            
            // 확장자 확인 및 추가
            String extension = "";
            FileNameExtensionFilter filter = (FileNameExtensionFilter) fileChooser.getFileFilter();
            String description = filter.getDescription();
            
            // 파일 형식에 맞는 확장자 설정
            if (description.contains("PNG")) {
                extension = "png";
            } else if (description.contains("JPEG")) {
                extension = "jpg";
            } else if (description.contains("BMP")) {
                extension = "bmp";
            }
            
            // 확장자가 없으면 추가
            if (!fileName.toLowerCase().endsWith("." + extension)) {
                fileName += "." + extension;
                fileToSave = new File(fileName);
            }
            
            try {
                // 현재 그림 이미지 가져오기
                BufferedImage image = drawingPanel.getImage();
                // 파일로 저장
                ImageIO.write(image, extension, fileToSave);
                // 성공 메시지
                JOptionPane.showMessageDialog(this,
                        "이미지가 성공적으로 저장되었습니다.", "저장 성공",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                // 오류 메시지
                JOptionPane.showMessageDialog(this,
                        "이미지 저장 중 오류가 발생했습니다: " + e.getMessage(),
                        "저장 실패", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // 이미지 불러오는 메소드
    private void openImage() {
        // 파일 선택 대화상자 만들기
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("이미지 열기");
        fileChooser.setFileFilter(new FileNameExtensionFilter("이미지 파일", "png", "jpg", "jpeg", "bmp"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            // 선택한 파일
            File fileToLoad = fileChooser.getSelectedFile();
            try {
                // 이미지 파일 읽기
                BufferedImage loadedImage = ImageIO.read(fileToLoad);
                if (loadedImage != null) {
                    // 그림 패널에 이미지 설정
                    drawingPanel.setImage(loadedImage);
                } else {
                    // 지원되지 않는 형식일 때
                    JOptionPane.showMessageDialog(this,
                            "지원되지 않는 이미지 형식입니다.", "열기 실패",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                // 오류 메시지
                JOptionPane.showMessageDialog(this,
                        "이미지 열기 중 오류가 발생했습니다: " + e.getMessage(),
                        "열기 실패", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // 메인 메소드 - 프로그램 시작점
    public static void main(String[] args) {
        // 이벤트 디스패치 스레드에서 GUI 생성
        SwingUtilities.invokeLater(() -> new MainFrame());
    }
}