package wfm.models;

import org.json.JSONObject;

import static utils.Params.STANDARD;
import static utils.Params.FACT;
import static utils.Params.EMPLOYEE_POSITION_ID;

public class DeviationFromStandard {


    private final double standard;
    private final double fact;
    private final int employeePositionId;

    public DeviationFromStandard(double standard, double fact, int employeePositionId) {
        this.standard = standard;
        this.fact = fact;
        this.employeePositionId = employeePositionId;
    }

    public DeviationFromStandard(JSONObject deviation) {
        this.standard = deviation.getDouble(STANDARD);
        this.fact = deviation.getDouble(FACT);
        this.employeePositionId = deviation.getInt(EMPLOYEE_POSITION_ID);
    }

    public double getStandard() {
        return this.standard;
    }

    public double getFact() {
        return this.fact;
    }

    public int getEmployeePositionId() {
        return this.employeePositionId;
    }

    public double getDeviation() {
        return Math.round((fact - standard) * 100d) / 100d;
    }


    @Override
    public String toString() {
        return Double.toString(this.standard) + " " + Double.toString(this.fact) + " " + Integer.toString(this.employeePositionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviationFromStandard deviation = (DeviationFromStandard) o;

        return deviation.toString().equals(this.toString());
    }
}