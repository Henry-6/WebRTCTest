package com.rd.webrtctest;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

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
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

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

    public void create(EglBase eglBase, SurfaceViewRenderer surfaceViewRenderer, String playUrl, WebRtcCallBack callBack) {
        this.eglBase = eglBase;
        this.surfaceViewRenderer = surfaceViewRenderer;
        this.callBack = callBack;
        this.playUrl = playUrl;

        init(true);
    }

    private void init(boolean isShowVideo) {
        peerConnectionFactory = getPeerConnectionFactory(context);
        // NOTE: this _must_ happen while PeerConnectionFactory is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);

        if (!isShowVideo) {
            peerConnection = peerConnectionFactory.createPeerConnection(getConfig(), this);

            //设置仅接收音视频
            peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY));
            peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY));
        }
        peerConnection.createOffer(this, new MediaConstraints());
    }

    public void destroy() {
        if (callBack != null) {
            callBack = null;
        }
        if (surfaceViewRenderer != null) {
            surfaceViewRenderer.release();
            surfaceViewRenderer = null;
        }
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
    }

    private int reConnCount;
    private final int MAX_CONN_COUNT = 10;

    public void openWebRtc(String sdp) {
        RxHttp.postJson("xxxx")
                .add("api", "xxxx")
                .add("streamurl", playUrl)
                .add("clientip", null)
                .add("sdp", sdp)
                .asString()
                .subscribe(s -> {
                    Log.e("WebRtc流", "地址:" + playUrl + s);
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
        SessionDescription remoteSpd = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
        peerConnection.setRemoteDescription(this, remoteSpd);
    }

    public interface WebRtcCallBack {
        void onSuccess();
        void onFail();
    }

    private WebRtcCallBack callBack;

    /**
     * 获取 PeerConnectionFactory
     */
    private PeerConnectionFactory getPeerConnectionFactory(Context context) {
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();

        PeerConnectionFactory.initialize(initializationOptions);

        // 2. 设置编解码方式：默认方法
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
                eglBase.getEglBaseContext(),
                false,
                true);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

        // 构造Factory
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
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.NEGOTIATE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        rtcConfig.enableDtlsSrtp = true;
        rtcConfig.enableCpuOveruseDetection = false;
        //修改模式 PlanB无法使用仅接收音视频的配置
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        return rtcConfig;
    }

    @Override
    public void onCreateSuccess(SessionDescription sdp) {
        if (sdp.type == SessionDescription.Type.OFFER) {
            //设置setLocalDescription offer返回sdp
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
            if (surfaceViewRenderer != null) {
                ProxyVideoSink videoSink = new ProxyVideoSink();
                videoSink.setTarget(surfaceViewRenderer);
                remoteVideoTrack.addSink(videoSink);
            }
        }
    }
}
