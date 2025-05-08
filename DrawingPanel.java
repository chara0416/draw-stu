package draw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Stack;

class DrawingPanel extends JPanel {
    // 그림이 그려질 이미지
    private BufferedImage drawImage;
    // 그래픽스 객체 (그림 그리는 도구)
    private Graphics2D g2d;
    
    // 마우스 시작점과 끝점
    private Point startPoint;
    private Point endPoint;
    
    // 현재 그리는 도형
    private Shape currentShape;
    // 그리기 상태
    private boolean isDrawing = false;
    
    // 그리기 설정
    private Color color = Color.BLACK;  // 색상
    private int strokeSize = 3;         // 선 두께
    private MainFrame.DrawingTool currentTool = MainFrame.DrawingTool.PENCIL;  // 현재 도구
    
    // 자유 곡선용 포인트 저장 리스트
    private ArrayList<Point> freehandPoints = new ArrayList<>();
    
    // 부드러운 곡선을 위한 경로
    private Path2D currentPath;
    
    // 실행 취소/다시 실행을 위한 스택
    private Stack<BufferedImage> undoStack = new Stack<>();  // 실행 취소용 스택
    private Stack<BufferedImage> redoStack = new Stack<>();  // 다시 실행용 스택
    
    // 그림의 최대 기록 수
    private static final int MAX_UNDO = 20;
    
    // 부드러운 곡선을 위한 설정
    private static final int MIN_DISTANCE = 2;  // 최소 거리 (점이 너무 밀집되지 않도록)
    private static final int SMOOTHNESS = 3;    // 부드러움 정도 (높을수록 더 부드러움)
    
    // 생성자
    public DrawingPanel() {
        // 배경색 설정
        setBackground(Color.WHITE);
        
        // 마우스 이벤트 리스너 등록
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 마우스 버튼을 눌렀을 때
                startPoint = e.getPoint();  // 시작점 저장
                isDrawing = true;           // 그리기 상태 시작
                freehandPoints.clear();     // 포인트 리스트 초기화
                
                if (currentTool == MainFrame.DrawingTool.PENCIL || 
                    currentTool == MainFrame.DrawingTool.ERASER) {
                    // 새로운 경로 시작
                    currentPath = new Path2D.Float();
                    currentPath.moveTo(startPoint.x, startPoint.y);
                    
                    freehandPoints.add(startPoint);
                    saveForUndo();  // 현재 상태 저장
                    
                    // 지우개면 흰색, 아니면 선택된 색상
                    if (currentTool == MainFrame.DrawingTool.ERASER) {
                        g2d.setColor(Color.WHITE);
                    } else {
                        g2d.setColor(color);
                    }
                    
                    // 시작점에 점 찍기
                    g2d.setStroke(new BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.fillOval(startPoint.x - strokeSize/2, startPoint.y - strokeSize/2, strokeSize, strokeSize);
                    repaint();
                } else if (currentTool == MainFrame.DrawingTool.TEXT) {
                    // 텍스트 입력을 위한 대화상자 표시
                    String text = JOptionPane.showInputDialog(DrawingPanel.this, "텍스트를 입력하세요:");
                    if (text != null && !text.isEmpty()) {
                        saveForUndo();
                        g2d.setColor(color);
                        // 글꼴 설정 (선 두께에 비례한 크기)
                        g2d.setFont(new Font("맑은 고딕", Font.PLAIN, strokeSize * 5));
                        g2d.drawString(text, startPoint.x, startPoint.y);
                        repaint();
                    }
                    isDrawing = false;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // 마우스를 드래그할 때
                if (!isDrawing) return;
                
                endPoint = e.getPoint();  // 현재 위치 저장
                
                if (currentTool == MainFrame.DrawingTool.PENCIL || 
                    currentTool == MainFrame.DrawingTool.ERASER) {
                    // 자유 곡선 그리기
                    Point lastPoint = freehandPoints.get(freehandPoints.size() - 1);
                    
                    // 최소 거리 체크 (포인트가 너무 밀집되지 않도록)
                    double distance = lastPoint.distance(endPoint);
                    if (distance < MIN_DISTANCE) {
                        return;
                    }
                    
                    freehandPoints.add(endPoint);
                    
                    // 지우개면 흰색, 아니면 선택된 색상
                    if (currentTool == MainFrame.DrawingTool.ERASER) {
                        g2d.setColor(Color.WHITE);
                    } else {
                        g2d.setColor(color);
                    }
                    
                    g2d.setStroke(new BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    
                    // 부드러운 곡선을 위해 경로에 추가
                    if (freehandPoints.size() >= 3) {
                        // 충분한 포인트가 있을 때 부드러운 베지어 곡선 그리기
                        drawSmoothLine(g2d);
                    } else {
                        // 포인트가 충분하지 않을 때는 직선으로 연결
                        g2d.drawLine(lastPoint.x, lastPoint.y, endPoint.x, endPoint.y);
                        currentPath.lineTo(endPoint.x, endPoint.y);
                    }
                    
                    repaint();
                } else {
                    // 임시 그리기 (미리보기)
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // 마우스 버튼을 뗐을 때
                if (!isDrawing) return;
                
                endPoint = e.getPoint();
                
                if (currentTool == MainFrame.DrawingTool.PENCIL || 
                    currentTool == MainFrame.DrawingTool.ERASER) {
                    // 마지막 점 추가
                    if (!freehandPoints.isEmpty() && !endPoint.equals(freehandPoints.get(freehandPoints.size() - 1))) {
                        freehandPoints.add(endPoint);
                        
                        // 최종 경로 그리기
                        if (freehandPoints.size() >= 3) {
                            if (currentTool == MainFrame.DrawingTool.ERASER) {
                                g2d.setColor(Color.WHITE);
                            } else {
                                g2d.setColor(color);
                            }
                            g2d.setStroke(new BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            drawSmoothLine(g2d);
                        }
                    }
                } else if (currentTool != MainFrame.DrawingTool.TEXT) {
                    // 도형 그리기 (직선, 사각형, 원)
                    saveForUndo();
                    draw();
                }
                
                isDrawing = false;
                repaint();
            }
        };
        
        // 마우스 이벤트 리스너 등록
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        
        // 초기화
        createDrawImage();
    }
    
    // 그리기 이미지 생성
    private void createDrawImage() {
        // 새 이미지 생성 (처음에는 기본 크기로)
        drawImage = new BufferedImage(getWidth() > 0 ? getWidth() : 800, 
                                     getHeight() > 0 ? getHeight() : 600, 
                                     BufferedImage.TYPE_INT_ARGB);
        // 그래픽스 객체 가져오기
        g2d = drawImage.createGraphics();
        // 부드러운 그리기 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 배경 흰색으로 칠하기
        g2d.setPaint(Color.WHITE);
        g2d.fillRect(0, 0, drawImage.getWidth(), drawImage.getHeight());
        // 그리기 설정
        g2d.setPaint(color);
        g2d.setStroke(new BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // 처음 상태 저장
        saveForUndo();
    }
    
    // 그림 지우기
    public void clear() {
        saveForUndo();
        // 흰색으로 모두 칠하기
        g2d.setPaint(Color.WHITE);
        g2d.fillRect(0, 0, drawImage.getWidth(), drawImage.getHeight());
        // 원래 색상으로 돌아가기
        g2d.setPaint(color);
        repaint();
    }
    
    // 도형 그리기 메소드
    private void draw() {
        if (startPoint == null || endPoint == null) return;
        
        // 색상과 선 설정
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // 도구에 따라 다른 그리기 수행
        switch (currentTool) {
            case LINE: // 직선
                g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
                break;
            case RECTANGLE: // 사각형
                // 시작점과 끝점 중 작은 값을 왼쪽 위 좌표로 사용
                int x = Math.min(startPoint.x, endPoint.x);
                int y = Math.min(startPoint.y, endPoint.y);
                // 너비와 높이 계산
                int width = Math.abs(startPoint.x - endPoint.x);
                int height = Math.abs(startPoint.y - endPoint.y);
                g2d.drawRect(x, y, width, height);
                break;
            case OVAL: // 원
                // 시작점과 끝점 중 작은 값을 왼쪽 위 좌표로 사용
                x = Math.min(startPoint.x, endPoint.x);
                y = Math.min(startPoint.y, endPoint.y);
                // 너비와 높이 계산
                width = Math.abs(startPoint.x - endPoint.x);
                height = Math.abs(startPoint.y - endPoint.y);
                g2d.drawOval(x, y, width, height);
                break;
        }
    }
    
    /**
     * 부드러운 곡선 그리기 메소드
     * 베지어 곡선을 이용하여 부드러운 곡선을 그립니다.
     */
    private void drawSmoothLine(Graphics2D g2d) {
        // 포인트가 3개 미만이면 그리지 않음
        int numPoints = freehandPoints.size();
        if (numPoints < 3) return;
        
        // 새 부드러운 경로 생성
        Path2D smoothPath = new Path2D.Float();
        
        // 첫 점을 시작점으로 설정
        Point p0 = freehandPoints.get(0);
        smoothPath.moveTo(p0.x, p0.y);
        
        // 각 점들을 이용해 부드러운 곡선 그리기
        for (int i = 1; i < numPoints - 1; i++) {
            Point p1 = freehandPoints.get(i);     // 현재 점
            Point p2 = freehandPoints.get(i + 1); // 다음 점
            
            // 이전 점과 현재 점 사이의 중간점 계산
            float cx1 = (p0.x + p1.x) / 2.0f;
            float cy1 = (p0.y + p1.y) / 2.0f;
            
            // 현재 점과 다음 점 사이의 중간점 계산
            float cx2 = (p1.x + p2.x) / 2.0f;
            float cy2 = (p1.y + p2.y) / 2.0f;
            
            // 곡선 그리기 (두 중간점을 연결하는 곡선으로, 현재 점을 제어점으로 사용)
            smoothPath.quadTo(p1.x, p1.y, cx2, cy2);
            
            // 다음 반복을 위해 이전 점 업데이트
            p0 = p1;
        }
        
        // 마지막 점 추가
        Point lastPoint = freehandPoints.get(numPoints - 1);
        smoothPath.lineTo(lastPoint.x, lastPoint.y);
        
        // 부드러운 곡선 그리기
        g2d.draw(smoothPath);
        
        // 현재 패스 업데이트
        currentPath = smoothPath;
    }
    
    // 그림 그리기 (화면에 보여주는 메소드, 자동 호출됨)
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 필요한 경우 이미지 크기 조정
        if (drawImage == null || drawImage.getWidth() != getWidth() || drawImage.getHeight() != getHeight()) {
            // 새 이미지 생성
            BufferedImage newImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D newG2d = newImage.createGraphics();
            
            // 부드러운 그리기 설정
            newG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            newG2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            newG2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // 기존 이미지가 있으면 복사
            if (drawImage != null) {
                newG2d.drawImage(drawImage, 0, 0, null);
            } else {
                // 없으면 흰색 배경 생성
                newG2d.setPaint(Color.WHITE);
                newG2d.fillRect(0, 0, getWidth(), getHeight());
            }
            
            // 새 이미지로 교체
            drawImage = newImage;
            g2d = newG2d;
            g2d.setPaint(color);
            g2d.setStroke(new BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        
        // 그림 그리기
        g.drawImage(drawImage, 0, 0, null);
        
        // 도형 미리보기 (드래그 중일 때)
        if (isDrawing && startPoint != null && endPoint != null) {
            // 미리보기용 그래픽스 생성
            Graphics2D previewG2D = (Graphics2D) g.create();
            previewG2D.setColor(color);
            previewG2D.setStroke(new BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // 부드러운 그리기 설정
            previewG2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            previewG2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            previewG2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // 도구에 따라 다른 미리보기
            switch (currentTool) {
                case PENCIL:
                case ERASER:
                    // 현재 그리는 경로 미리보기
                    if (currentPath != null) {
                        previewG2D.draw(currentPath);
                    }
                    break;
                case LINE: // 직선
                    previewG2D.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
                    break;
                case RECTANGLE: // 사각형
                    int x = Math.min(startPoint.x, endPoint.x);
                    int y = Math.min(startPoint.y, endPoint.y);
                    int width = Math.abs(startPoint.x - endPoint.x);
                    int height = Math.abs(startPoint.y - endPoint.y);
                    previewG2D.drawRect(x, y, width, height);
                    break;
                case OVAL: // 원
                    x = Math.min(startPoint.x, endPoint.x);
                    y = Math.min(startPoint.y, endPoint.y);
                    width = Math.abs(startPoint.x - endPoint.x);
                    height = Math.abs(startPoint.y - endPoint.y);
                    previewG2D.drawOval(x, y, width, height);
                    break;
            }
            
            // 사용 후 그래픽스 해제
            previewG2D.dispose();
        }
    }
    
    // 실행 취소를 위한 현재 상태 저장
    public void saveForUndo() {
        // 현재 이미지 복사
        BufferedImage copy = new BufferedImage(drawImage.getWidth(), drawImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D copyG2d = copy.createGraphics();
        copyG2d.drawImage(drawImage, 0, 0, null);
        copyG2d.dispose();
        
        // 스택에 저장
        undoStack.push(copy);
        // 다시 실행 스택 비우기
        redoStack.clear();
        
        // 스택 크기 제한 (메모리 관리)
        if (undoStack.size() > MAX_UNDO) {
            undoStack.remove(0);
        }
    }
    
    // 실행 취소
    public void undo() {
        if (undoStack.size() > 1) {  // 처음 상태는 남겨둠
            // 현재 상태를 다시 실행 스택에 저장
            BufferedImage currentState = undoStack.pop();
            redoStack.push(currentState);
            
            // 이전 상태 복원
            BufferedImage previousState = undoStack.peek();
            g2d.drawImage(previousState, 0, 0, null);
            repaint();
        }
    }
    
    // 다시 실행
    public void redo() {
        if (!redoStack.empty()) {
            // 다시 실행 스택에서 상태 가져오기
            BufferedImage nextState = redoStack.pop();
            undoStack.push(nextState);
            
            // 상태 복원
            g2d.drawImage(nextState, 0, 0, null);
            repaint();
        }
    }
    
    // 이미지 설정 (불러오기용)
    public void setImage(BufferedImage image) {
        saveForUndo();
        
        // 새 이미지 복사
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, drawImage.getWidth(), drawImage.getHeight());
        g2d.drawImage(image, 0, 0, null);
        repaint();
    }
    
    // 현재 이미지 가져오기 (저장용)
    public BufferedImage getImage() {
        return drawImage;
    }
    
    // 도구 설정
    public void setTool(MainFrame.DrawingTool tool) {
        currentTool = tool;
    }
    
    // 색상 설정
    public void setColor(Color color) {
        this.color = color;
    }
    
    // 선 두께 설정
    public void setStrokeSize(int size) {
        strokeSize = size;
    }
}
