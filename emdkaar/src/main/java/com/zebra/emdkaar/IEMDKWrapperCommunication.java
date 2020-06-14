package com.zebra.emdkaar;

public interface IEMDKWrapperCommunication {

    void setStatus(String status);
    void setData(String dataString);
    void asyncUpdate(boolean b);
}
