package com.edgerealities.sdk.example.CSFragments;

public class PassedData {
    private String valvePollUrl;
    private int valveId;
    private float horizontalBias;
    private float verticalBias;

    public PassedData(PassedData passedData){
        this.valvePollUrl = passedData.valvePollUrl;

        this.valveId = passedData.valveId;
        this.horizontalBias = passedData.horizontalBias;
        this.verticalBias = passedData.verticalBias;
    }

    public PassedData(){
        this.valvePollUrl = "";

        this.valveId = 0;
        this.horizontalBias = 0.5f;
        this.verticalBias = 0.5f;
    }

    public int getValveId(){return valveId;}
    public float getHorizontalBias(){return horizontalBias;}
    public float getVerticalBias(){return verticalBias;}
    public String getValvePollUrl() {return valvePollUrl;}

    public void setValveId(int value){ valveId = value; }
    public void setHorizontalBias(float value){ horizontalBias = value; }
    public void setVerticalBias(float value){ verticalBias = value; }
    public void setValvePollUrl(String value) { this.valvePollUrl = value; }


    public void setValveData(int valveId, float hBias, float vBias){
        this.valveId = valveId;
        this.horizontalBias = hBias;
        this.verticalBias = vBias;
    }

    public void setPollUrls(String valvePollUrl){
        this.valvePollUrl = valvePollUrl;
    }
}
