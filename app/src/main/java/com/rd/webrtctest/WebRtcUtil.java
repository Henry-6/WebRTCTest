package com.rd.webrtctest;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.util.ArrayList;

import rxhttp.RxHttp;


/**
 * @author haimian on 2021/4/24 0024
 */
public class WebRtcUtil implements PeerConnection.Observer, SdpObserver {

    private Context context;

    public WebRtcUtil(Context context){
        this.context = context.getApplicationContext();
    }

    private EglBase eglBase;

    private String playUrl;

    private PeerConnection peerConnection;
    private SurfaceViewRenderer surfaceViewRenderer;
    private PeerConnectionFactory peerConnectionFactory;

    private AudioSource audioSource;
    private VideoSource videoSource;
    private AudioTrack localAudioTrack;
    private VideoTrack localVideoTrack;
    private VideoCapturer captureAndroid;
    private SurfaceTextureHelper surfaceTextureHelper;
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private boolean isShowCamera = false;
    private static final int VIDEO_RESOLUTION_WIDTH = 1280;
    private static final int VIDEO_RESOLUTION_HEIGHT = 720;
    private static final int FPS = 30;
    /**
     * isPublish true????????? false?????????
     */
    private boolean isPublish;

    public void create(EglBase eglBase, SurfaceViewRenderer surfaceViewRenderer, String playUrl, WebRtcCallBack callBack) {
        create(eglBase, surfaceViewRenderer, false, playUrl, callBack);
    }

    public void create(EglBase eglBase, SurfaceViewRenderer surfaceViewRenderer, boolean isPublish, String playUrl, WebRtcCallBack callBack) {
        this.eglBase = eglBase;
        this.surfaceViewRenderer = surfaceViewRenderer;
        this.callBack = callBack;
        this.playUrl = playUrl;
        this.isPublish = isPublish;

        init();
    }

    public void create(EglBase eglBase, SurfaceViewRenderer surfaceViewRenderer, boolean isPublish, boolean isShowCamera, String playUrl, WebRtcCallBack callBack) {
        this.eglBase = eglBase;
        this.surfaceViewRenderer = surfaceViewRenderer;
        this.callBack = callBack;
        this.playUrl = playUrl;
        this.isPublish = isPublish;
        this.isShowCamera = isShowCamera;

        init();
    }

    private void init() {
        peerConnectionFactory = getPeerConnectionFactory(context);
        // NOTE: this _must_ happen while PeerConnectionFactory is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_NONE);

        peerConnection = peerConnectionFactory.createPeerConnection(getConfig(), this);
        MediaConstraints mediaConstraints = new MediaConstraints();

        if (!isPublish) {
            //????????????????????????
            peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY));
            peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY));
        }
        else {
            //????????????????????????
            peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY));
            peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY));

            //??????????????????
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);

            // ??????
            audioSource = peerConnectionFactory.createAudioSource(createAudioConstraints());
            localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
            localAudioTrack.setEnabled(true);

            peerConnection.addTrack(localAudioTrack);
            //???????????????????????????
            if (isShowCamera) {
                captureAndroid = CameraUtil.createVideoCapture(context);
                surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());

                videoSource = peerConnectionFactory.createVideoSource(false);

                captureAndroid.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
                captureAndroid.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

                localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
                localVideoTrack.setEnabled(true);
                if (surfaceViewRenderer != null) {
                    ProxyVideoSink videoSink = new ProxyVideoSink();
                    videoSink.setTarget(surfaceViewRenderer);
                    localVideoTrack.addSink(videoSink);
                }
                peerConnection.addTrack(localVideoTrack);
            }
        }
        peerConnection.createOffer(this, mediaConstraints);
    }

    public void destroy() {
        if (callBack != null) {
            callBack = null;
        }
        if (peerConnection != null) {
            peerConnection.dispose();
            peerConnection = null;
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        if (captureAndroid != null) {
            captureAndroid.dispose();
            captureAndroid = null;
        }
        if (surfaceViewRenderer != null) {
            surfaceViewRenderer.clearImage();
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
    }

    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";

    /**
     * ??????????????????
     * @return
     */
    private MediaConstraints createAudioConstraints() {
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        return audioConstraints;
    }


    private int reConnCount;
    private final int MAX_CONN_COUNT = 10;

    public void openWebRtc(String sdp) {
        //isPublish true???xxxx???url????????????publish false???xxxx???url?????????play
        //???: "https://www.baidu.com/rtc/v1/publish" : "https://www.baidu.com/rtc/v1/play"
        //?????????url???api???????????????????????????
        RxHttp.postBody(playUrl, sdp)
//                .add("app", "live")
//                .add("stream", "test")
//                .add("type", isPublish ? "push" : "play")
                .setBody(sdp, null)
                .asString()
                .subscribe(s -> {
                    s = s.replaceAll("\n", "");
                    Log.e("WebRtc???", "????????????: " + isPublish + "  ??????:" + playUrl + s);
                    if (!TextUtils.isEmpty(s)) {
                        SdpBean sdpBean = new Gson().fromJson(s, SdpBean.class);
                        if (sdpBean.getCode() == 400) {
                            openWebRtc(sdp);
                            return;
                        }
                        if (!TextUtils.isEmpty(sdpBean.getSdp())) {
                            setRemoteSdp(sdpBean.getSdp());
                        }
                    }
                }, throwable -> {
                    openWebRtc(sdp);
                });
    }

    public void setRemoteSdp(String sdp) {
        if (peerConnection != null) {
            SessionDescription remoteSpd = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
            peerConnection.setRemoteDescription(this, remoteSpd);
        }
    }

    public interface WebRtcCallBack {
        void onSuccess();
        void onFail();
    }

    private WebRtcCallBack callBack;

    /**
     * ?????? PeerConnectionFactory
     */
    private PeerConnectionFactory getPeerConnectionFactory(Context context) {
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();

        PeerConnectionFactory.initialize(initializationOptions);

        // 2. ????????????????????????????????????
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
                eglBase.getEglBaseContext(),
                false,
                true);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

        // ??????Factory
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                .builder(context)
                .createInitializationOptions());

        return PeerConnectionFactory.builder()
                .setOptions(new PeerConnectionFactory.Options())
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(context).createAudioDeviceModule())
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    private PeerConnection.RTCConfiguration getConfig() {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(new ArrayList<>());
        //?????????????????????
        rtcConfig.enableCpuOveruseDetection = false;
        //???????????? PlanB???????????????????????????????????????
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        return rtcConfig;
    }

    @Override
    public void onCreateSuccess(SessionDescription sdp) {
        if (sdp.type == SessionDescription.Type.OFFER) {
            //??????setLocalDescription offer??????sdp
            peerConnection.setLocalDescription(this, sdp);
            if (!TextUtils.isEmpty(sdp.description)) {
                reConnCount = 0;
                openWebRtc(sdp.description);
            }
        }
    }

    @Override
    public void onSetSuccess() {

    }

    @Override
    public void onCreateFailure(String error) {

    }

    @Override
    public void onSetFailure(String error) {

    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState newState) {

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {

    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {

    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        peerConnection.addIceCandidate(candidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        peerConnection.removeIceCandidates(candidates);
    }

    @Override
    public void onAddStream(MediaStream stream) {

    }

    @Override
    public void onRemoveStream(MediaStream stream) {

    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
        MediaStreamTrack track = receiver.track();
        if (track instanceof VideoTrack) {
            VideoTrack remoteVideoTrack = (VideoTrack) track;
            remoteVideoTrack.setEnabled(true);
            if (surfaceViewRenderer != null && isShowCamera) {
                ProxyVideoSink videoSink = new ProxyVideoSink();
                videoSink.setTarget(surfaceViewRenderer);
                remoteVideoTrack.addSink(videoSink);
            }
        }
    }
}
