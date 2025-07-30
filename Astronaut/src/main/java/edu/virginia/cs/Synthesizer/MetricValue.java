package edu.virginia.cs.Synthesizer;

public class MetricValue {
    private Integer TATI = 0;
    private Integer NCT = 0;
    private Integer NCRF = 0;
    private Integer ANV = 0;
    private Integer NIC = 0;
    private Integer NFK = 0;

    private String TATI_detail = "";
    private String NCT_detail = "";
    private String NCRF_detail = "";
    private String ANV_detail = "";

    public Integer getTATI() {
        return TATI;
    }

    public void setTATI(Integer TATI) {
        this.TATI = TATI;
    }

    public Integer getNCT() {
        return NCT;
    }

    public void setNCT(Integer NCT) {
        this.NCT = NCT;
    }

    public Integer getNCRF() {
        return NCRF;
    }

    public void setNCRF(Integer NCRF) {
        this.NCRF = NCRF;
    }

    public Integer getANV() {
        return ANV;
    }

    public void setANV(Integer ANV) {
        this.ANV = ANV;
    }

    public boolean equals(MetricValue m) {
        return TATI.equals(m.getTATI()) && NCT.equals(m.getNCT()) && NCRF.equals(m.getNCRF()) && ANV.equals(m.getANV()) &&
                NFK.equals(m.getNFK()) && TATI_detail.equals(m.TATI_detail) && NCT_detail.equals(m.NCT_detail) && NCRF_detail.equals(m.NCRF_detail) && ANV_detail.equals(m.ANV_detail);
    }

    public String getTATI_detail() {
        return TATI_detail;
    }

    public void setTATI_detail(String TATI_detail) {
        this.TATI_detail = TATI_detail;
    }

    public String getNCT_detail() {
        return NCT_detail;
    }

    public void setNCT_detail(String NCT_detail) {
        this.NCT_detail = NCT_detail;
    }

    public String getNCRF_detail() {
        return NCRF_detail;
    }

    public void setNCRF_detail(String NCRF_detail) {
        this.NCRF_detail = NCRF_detail;
    }

    public String getANV_detail() {
        return ANV_detail;
    }

    public void setANV_detail(String ANV_detail) {
        this.ANV_detail = ANV_detail;
    }

    public Integer getNIC() {
        return NIC;
    }

    public void setNIC(Integer NIC) {
        this.NIC = NIC;
    }

    public Integer getNFK() {
        return NFK;
    }

    public void setNFK(Integer NFK) {
        this.NFK = NFK;
    }
}
