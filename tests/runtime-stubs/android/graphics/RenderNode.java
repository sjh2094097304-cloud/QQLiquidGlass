package android.graphics;
public final class RenderNode {
    public RenderNode(String name){}
    public void setPosition(int l,int t,int r,int b){}
    public RecordingCanvas beginRecording(int w,int h){return new RecordingCanvas();}
    public void endRecording(){}
    public void discardDisplayList(){}
}
