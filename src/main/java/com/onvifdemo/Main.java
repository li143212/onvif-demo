package com.onvifdemo;


import de.onvif.soap.OnvifDevice;
import de.onvif.soap.SOAP;
import de.onvif.soap.devices.MediaDevices;
import de.onvif.soap.devices.PtzDevices;
import org.onvif.ver10.device.wsdl.GetNetworkInterfaces;
import org.onvif.ver10.device.wsdl.GetNetworkInterfacesResponse;
import org.onvif.ver10.schema.*;

import javax.xml.soap.SOAPException;
import java.net.ConnectException;
import java.util.List;
import java.util.Optional;

/**
 * @author lihe
 * @date 2024/9/6 下午2:52
 */
//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {

    public static void main(String[] args) throws SOAPException, ConnectException, InterruptedException {
        String defaultPresetName = "20";
        String cameraIp = "10.25.XX.XXX";
        String username = "admin";
        String password = "123456";
        int channelNum = 1;
        String channelName = "Channel"+channelNum;
        OnvifDevice onvifDevice = new OnvifDevice(cameraIp, username, password);
        onvifDevice.getSoap().setLogging(false);
        List<Profile> profiles = onvifDevice.getDevices().getProfiles();
        for (Profile profile : profiles) {
            System.out.println("---------------------------------"+profile.getName()+"---------------------------------\n");
            if (profile.getName().contains(channelName) && profile.getName().contains("MainStream")) {
                // 通道1主码流
                String profileToken = profile.getToken();
                System.out.println("profileToken:  "+profileToken);
                StreamSetup setup = new StreamSetup();
                setup.setStream(StreamType.RTP_UNICAST);
                Transport transport = new Transport();
                transport.setProtocol(TransportProtocol.RTSP);
                setup.setTransport(transport);
                MediaDevices mediaDevices = onvifDevice.getMedia();
                String RTSPUrl = mediaDevices.getStreamUri(profileToken, setup);
                int index = RTSPUrl.indexOf("://") + 3; // 获取到 rtsp:// 之后的索引

                // 插入 account:password@ 到 ip:port 之前
                String modifiedRtspUrl = RTSPUrl.substring(0, index) + username + ":" + password + "@" + RTSPUrl.substring(index);
                System.out.println("modifiedRtspUrl:"+modifiedRtspUrl);
                SOAP soap = new SOAP(onvifDevice);
                GetNetworkInterfacesResponse response = new GetNetworkInterfacesResponse();
                soap.createSOAPDeviceRequest(new GetNetworkInterfaces(), response, true);
                System.out.println(response);
                PtzDevices ptzDevices = onvifDevice.getPtz();
                // 设置预置点
                ptzDevices.setPreset(defaultPresetName, profileToken);
                Thread.sleep(3000);
                // 连续移动
                ptzDevices.continuousMove(profileToken, -0.5F, -0.5F, 0);
                Thread.sleep(3000);
                // 停止移动
                ptzDevices.stopMove(profileToken);
                List<PTZPreset> presets = ptzDevices.getPresets(profileToken);
                System.out.println(presets.toString());
                if (!presets.isEmpty()) {
                    Optional<PTZPreset> preset = presets.stream()
                            .filter(p -> p.getName().equals(defaultPresetName))
                            .findFirst();
                    if (preset.isPresent()) {
                        // 移动到预置点
                        ptzDevices.gotoPreset(preset.get().getToken(), profileToken);
                        System.out.println("已到达预置点"+defaultPresetName);
                    }
                }
            }
        }


    }


}