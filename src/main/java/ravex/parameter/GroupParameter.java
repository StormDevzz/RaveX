package ravex.parameter;

public class GroupParameter extends BooleanParameter {
    private float chevronAngle = 0f;
    private float targetChevronAngle = 0f;

    public GroupParameter(String name, boolean defaultValue) {
        super(name, defaultValue);
        this.targetChevronAngle = defaultValue ? 90f : 0f;
        this.chevronAngle = targetChevronAngle;
    }

    public float getChevronAngle() {
        return chevronAngle;
    }

    public void updateChevron(float speed) {
        targetChevronAngle = getValue() ? 90f : 0f;
        chevronAngle += (targetChevronAngle - chevronAngle) * Math.min(1f, speed);
        if (Math.abs(chevronAngle - targetChevronAngle) < 0.1f) {
            chevronAngle = targetChevronAngle;
        }
    }
}
