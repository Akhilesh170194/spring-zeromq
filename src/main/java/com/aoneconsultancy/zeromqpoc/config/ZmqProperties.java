package com.aoneconsultancy.zeromqpoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zeromq")
public class ZmqProperties {

    /** Address where push socket will bind, e.g. tcp://*:5555 */
    private String pushBindAddress = "tcp://*:5555";

    /** Address where pull socket connects to, e.g. tcp://localhost:5555 */
    private String pullConnectAddress = "tcp://localhost:5555";

    /** High water mark / buffer size for sockets */
    private int bufferSize = 1000;

    public String getPushBindAddress() {
        return pushBindAddress;
    }

    public void setPushBindAddress(String pushBindAddress) {
        this.pushBindAddress = pushBindAddress;
    }

    public String getPullConnectAddress() {
        return pullConnectAddress;
    }

    public void setPullConnectAddress(String pullConnectAddress) {
        this.pullConnectAddress = pullConnectAddress;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
