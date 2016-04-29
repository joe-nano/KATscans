package no.uib.inf252.katscan.view.katview.opengl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.swing.Timer;
import no.uib.inf252.katscan.data.VoxelMatrix;
import no.uib.inf252.katscan.project.displayable.Displayable;
import no.uib.inf252.katscan.util.DisplayObject;
import no.uib.inf252.katscan.util.MatrixUtil;
import no.uib.inf252.katscan.util.TrackBall;
import no.uib.inf252.katscan.view.katview.KatView;

/**
 *
 * @author Marcelo Lima
 */
public abstract class VolumeRenderer extends GLJPanel implements KatView, GLEventListener {
    
    private static final String SHADERS_ROOT = "/shaders";
    private final String shaderName;
    
    private static final int BUFFER_VERTICES = 0;
    private static final int BUFFER_INDICES = 1;

    private static final int TEXTURE_VOLUME = 0;
    private static final int TEXTURE_FRAME_BUFFER = 1;
    protected static final int TEXTURE_COUNT_PARENT = 2;
    
    private static final int FRAME_BUFFER_FRONT = 0;
    
    private final int[] bufferLocation;
    private final int[] textureLocation;
    private final int[] frameBuffer;
    
    protected final Displayable displayable;
    
    private final TrackBall trackBall;
    private final DisplayObject displayObject;
    
    protected int programName;

    private int numSample;
    
    private Timer threadLOD;
    private boolean highLOD;
    
    private float[] tempMatrix;

    VolumeRenderer(Displayable displayable, String shaderName) throws GLException {
        super(new GLCapabilities(GLProfile.get(GLProfile.GL2)));
        addGLEventListener(this);
        
        bufferLocation = new int[2];
        textureLocation = new int[2];
        frameBuffer = new int[1];
        
        this.shaderName = shaderName;
        this.displayable = displayable;

        trackBall = new TrackBall(2 * displayable.getMatrix().getRatio()[2]);
        trackBall.installTrackBall(this);

        displayObject = DisplayObject.getObject(DisplayObject.Type.CUBE);
        
        numSample = 256;
        
        threadLOD = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                highLOD = true;
                repaint();
            }
        });
        threadLOD.setRepeats(false);
        
        tempMatrix = new float[16];
    }
    
    abstract protected void preDraw(GLAutoDrawable drawable);

    @Override
    public void init(GLAutoDrawable drawable) {
        VoxelMatrix voxelMatrix = displayable.getMatrix();
        if (voxelMatrix == null) {
            throw new GLException("Could not load the volume data");
        }
        
        trackBall.markAllDirty();
        highLOD = true;
        
        GL2 gl2 = drawable.getGL().getGL2();
        loadVertices(gl2);
        loadTexture(gl2, voxelMatrix);
        loadFrameBuffer(gl2);
        loadProgram(gl2);
        loadInitialUniforms(gl2, voxelMatrix);
    }

    private void loadInitialUniforms(GL2 gl2, VoxelMatrix voxelMatrix) {
        gl2.glUseProgram(programName);
        
        gl2.glBindFragDataLocation(programName, 0, "fragColor");
        gl2.glBindAttribLocation(programName, 0, "position");
        
        int location = gl2.glGetUniformLocation(programName, "numSamples");
        gl2.glUniform1i(location, numSample);

        location = gl2.glGetUniformLocation(programName, "ratio");
        gl2.glUniform3fv(location, 1, voxelMatrix.getRatio(), 0);

        location = gl2.glGetUniformLocation(programName, "volumeTexture");
        gl2.glUniform1i(location, 0);

        checkError(gl2, "Load initial uniforms");
    }

    private void loadFrameBuffer(GL2 gl2) {
//        gl2.glGenFramebuffers(1, frameBuffer, FRAME_BUFFER_FRONT);
//        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBuffer[FRAME_BUFFER_FRONT]);
//        
//        gl2.glGenTextures(1, textureLocation, TEXTURE_FRAME_BUFFER);
//        gl2.glActiveTexture(GL2.GL_TEXTURE0 + TEXTURE_FRAME_BUFFER);
//        gl2.glBindTexture(GL2.GL_TEXTURE_2D, textureLocation[TEXTURE_FRAME_BUFFER]);
//        
//        gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
//        gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
//        
//        gl2.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB, getWidth(), getHeight(), 0, GL2.GL_RGB, GL2.GL_UNSIGNED_BYTE, 0);
//        
//        if (gl2.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) != GL2.GL_FRAMEBUFFER_COMPLETE) {
//            throw new GLException("Failed to load frame buffer");
//        }
//        
//        checkError(gl2, "Load FrameBuffer");
    }

    private void loadVertices(GL2 gl2) {
        gl2.glGenBuffers(bufferLocation.length, bufferLocation, 0);
        
        float[] vertices = displayObject.getVertices();
        gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferLocation[BUFFER_VERTICES]);
        gl2.glBufferData(GL2.GL_ARRAY_BUFFER, vertices.length * Float.BYTES, FloatBuffer.wrap(vertices), GL2.GL_STATIC_DRAW);
        
        short[] indices = displayObject.getIndices();
        gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, bufferLocation[BUFFER_INDICES]);
        gl2.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indices.length * Short.BYTES, ShortBuffer.wrap(indices), GL2.GL_STATIC_DRAW);
        
        checkError(gl2, "Load vertices");
    }

    private void loadProgram(GL2 gl2) throws GLException {
        ShaderCode vertShader = ShaderCode.create(gl2, GL2.GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT,
                null, shaderName, true);
        ShaderCode fragShader = ShaderCode.create(gl2, GL2.GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT,
                null, shaderName, true);
        
        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);
        shaderProgram.init(gl2);
        
        programName = shaderProgram.program();
        shaderProgram.link(gl2, System.out);
        checkError(gl2, "Load and compile program");
    }

    private void loadTexture(GL2 gl2, VoxelMatrix voxelMatrix) throws RuntimeException {
        short[] texture = voxelMatrix.getData();
        
        numSample = (int) Math.sqrt(voxelMatrix.getSizeX() * voxelMatrix.getSizeX()
                + voxelMatrix.getSizeY() * voxelMatrix.getSizeY()
                + voxelMatrix.getSizeZ() * voxelMatrix.getSizeZ());
        
        gl2.glGenTextures(1, textureLocation, TEXTURE_VOLUME);
        gl2.glActiveTexture(GL2.GL_TEXTURE0 + TEXTURE_VOLUME);
        gl2.glBindTexture(GL2.GL_TEXTURE_3D, textureLocation[TEXTURE_VOLUME]);
        gl2.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl2.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl2.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_BORDER);
        gl2.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
        gl2.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
        gl2.glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL2.GL_RED, voxelMatrix.getSizeX(), voxelMatrix.getSizeY(), voxelMatrix.getSizeZ(), 0, GL2.GL_RED, GL2.GL_UNSIGNED_SHORT, ShortBuffer.wrap(texture));
        checkError(gl2, "Create Texture");
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl2 = drawable.getGL().getGL2();
        
        gl2.glDeleteProgram(programName);
        gl2.glDeleteBuffers(bufferLocation.length, bufferLocation, 0);
        
        gl2.glDeleteTextures(textureLocation.length, textureLocation, 0);
        checkError(gl2, "Dispose");
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        int uniformLocation;
        GL2 gl2 = drawable.getGL().getGL2();
        initializeRender(gl2);
        
        if (highLOD) {
            uniformLocation = gl2.glGetUniformLocation(programName, "lodMultiplier");
            gl2.glUniform1i(uniformLocation, 16);
        }
        
        checkAndLoadUpdates(gl2);        
        preDraw(drawable);
        checkError(gl2, "Pre draw");        
        draw(gl2);
        
        if (highLOD) {
            uniformLocation = gl2.glGetUniformLocation(programName, "lodMultiplier");
            gl2.glUniform1i(uniformLocation, 1);
            highLOD = false;
        } else {
            threadLOD.restart();
        }        
    }

    private void draw(GL2 gl2) {
        gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferLocation[BUFFER_VERTICES]);
        gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, bufferLocation[BUFFER_INDICES]);
        gl2.glEnableVertexAttribArray(0);
        gl2.glVertexAttribPointer(0, 3, GL2.GL_FLOAT, false, 0, 0);
        
        gl2.glDrawElements(GL2.GL_TRIANGLES, displayObject.getIndices().length, GL2.GL_UNSIGNED_SHORT, 0);

        checkError(gl2, "Draw");
    }

    private void initializeRender(GL2 gl2) {
//        gl4.glClearColor(0.234375f, 0.24609375f, 0.25390625f,1.0f);
//        gl4.glClearColor(0.2f,0.2f,0.2f,1.0f);
        gl2.glClearColor(0f,0f,0f,1.0f);
        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

//        gl4.glEnable(GL2.GL_DEPTH_TEST);
        gl2.glEnable(GL2.GL_CULL_FACE);
        gl2.glCullFace(GL2.GL_BACK);
        gl2.glEnable(GL2.GL_BLEND);
        gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
//        gl2.glBlendFunc(GL2.GL_ONE_MINUS_DST_ALPHA, GL2.GL_ONE);

        gl2.glUseProgram(programName);   

        checkError(gl2, "Initialize render");
    }

    private void checkAndLoadUpdates(GL2 gl2) {
        int uniformLocation;
        int dirtyValues = trackBall.getDirtyValues();
        if ((dirtyValues & (TrackBall.PROJECTION_DIRTY | TrackBall.VIEW_DIRTY | TrackBall.MODEL_DIRTY | TrackBall.ORTHO_DIRTY)) != 0) {

            if ((dirtyValues & TrackBall.PROJECTION_DIRTY) > 0) {
                uniformLocation = gl2.glGetUniformLocation(programName, "projection");
                gl2.glUniformMatrix4fv(uniformLocation, 1, false, trackBall.getProjectionMatrix(), 0);
                trackBall.clearDirtyValues(TrackBall.PROJECTION_DIRTY);
            }

            float[] viewMatrix = null;
            float[] modelMatrix = null;
            if ((dirtyValues & (TrackBall.VIEW_DIRTY | TrackBall.MODEL_DIRTY)) > 0) {
                uniformLocation = gl2.glGetUniformLocation(programName, "normalMatrix");
                if (uniformLocation >= 0) {
                    viewMatrix = trackBall.getViewMatrix();
                    modelMatrix = trackBall.getModelMatrix();
                
                    float[] normalMatrix = MatrixUtil.multiply(viewMatrix, modelMatrix);
                    MatrixUtil.getInverse(normalMatrix);
                    normalMatrix = FloatUtil.transposeMatrix(normalMatrix, tempMatrix);
                    normalMatrix = MatrixUtil.getMatrix3(normalMatrix);
                    gl2.glUniformMatrix3fv(uniformLocation, 1, false, normalMatrix, 0);
                }
            }
            
            if ((dirtyValues & TrackBall.VIEW_DIRTY) > 0) {
                if (viewMatrix == null) viewMatrix = trackBall.getViewMatrix();
                uniformLocation = gl2.glGetUniformLocation(programName, "view");
                gl2.glUniformMatrix4fv(uniformLocation, 1, false, viewMatrix, 0);                
                uniformLocation = gl2.glGetUniformLocation(programName, "eyePos");
                gl2.glUniform3fv(uniformLocation, 1, trackBall.getEyePosition(), 0);
                
                trackBall.clearDirtyValues(TrackBall.VIEW_DIRTY);
                trackBall.clearDirtyValues(TrackBall.ZOOM_DIRTY);
                trackBall.clearDirtyValues(TrackBall.FOV_DIRTY);
            }

            if ((dirtyValues & TrackBall.MODEL_DIRTY) > 0) {
                if (modelMatrix == null) modelMatrix = trackBall.getModelMatrix();
                uniformLocation = gl2.glGetUniformLocation(programName, "model");
                gl2.glUniformMatrix4fv(uniformLocation, 1, false, modelMatrix, 0);
                uniformLocation = gl2.glGetUniformLocation(programName, "invModel");
                gl2.glUniformMatrix3fv(uniformLocation, 1, false, MatrixUtil.getMatrix3(MatrixUtil.getInverse(modelMatrix)), 0);
                trackBall.clearDirtyValues(TrackBall.MODEL_DIRTY);
            }

            if ((dirtyValues & TrackBall.ORTHO_DIRTY) > 0) {
                uniformLocation = gl2.glGetUniformLocation(programName, "orthographic");
                gl2.glUniform1i(uniformLocation, trackBall.isOrthographic() ? 1 : 0);
                trackBall.clearDirtyValues(TrackBall.ORTHO_DIRTY);
            }
            checkError(gl2, "Update dirty values");
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        trackBall.updateProjection(width, height);
        //TODO Resize
//        GL2 gl2 = drawable.getGL().getGL2();
//        gl2.glActiveTexture(GL2.GL_TEXTURE0 + TEXTURE_TRANSFER);
//        gl2.glBindTexture(GL2.GL_TEXTURE_1D, textureLocation[0]);
//        gl2.glTexImage1D(GL2.GL_TEXTURE_1D, 0, GL2.GL_RGBA, TransferFunction.TEXTURE_SIZE, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, ByteBuffer.wrap(dataElements));
    }
    
    protected void checkError(GL2 gl, String location) {

        int error = gl.glGetError();
        if (error != GL2.GL_NO_ERROR) {
            String errorString;
            switch (error) {
                case GL2.GL_INVALID_ENUM:
                    errorString = "GL_INVALID_ENUM";
                    break;
                case GL2.GL_INVALID_VALUE:
                    errorString = "GL_INVALID_VALUE";
                    break;
                case GL2.GL_INVALID_OPERATION:
                    errorString = "GL_INVALID_OPERATION";
                    break;
                case GL2.GL_INVALID_FRAMEBUFFER_OPERATION:
                    errorString = "GL_INVALID_FRAMEBUFFER_OPERATION";
                    break;
                case GL2.GL_OUT_OF_MEMORY:
                    errorString = "GL_OUT_OF_MEMORY";
                    break;
                default:
                    errorString = "UNKNOWN";
                    break;
            }
            System.out.println(getClass().getSimpleName() + " :: OpenGL Error(" + errorString + "): " + location);
            throw new Error();
        }
    }
}