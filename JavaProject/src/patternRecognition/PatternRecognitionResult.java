package patternRecognition;

public class PatternRecognitionResult {

	private int seriesIndex;
	private int positionIndex;
	private float sadValue;

	public PatternRecognitionResult(int seriesIndex, int positionIndex, float sadValue) {
		this.seriesIndex = seriesIndex;
		this.positionIndex = positionIndex;
		this.sadValue = sadValue;
	}

	public int getSeriesIndex() {
		return seriesIndex;
	}

	public int getPositionIndex() {
		return positionIndex;
	}

	public float getSadValue() {
		return sadValue;
	}

	public void setSeriesIndex(int seriesIndex) {
		this.seriesIndex = seriesIndex;
	}

	public void setPositionIndex(int positionIndex) {
		this.positionIndex = positionIndex;
	}

	public void setSadValue(float sadValue) {
		this.sadValue = sadValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + positionIndex;
		result = prime * result + Float.floatToIntBits(sadValue);
		result = prime * result + seriesIndex;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PatternRecognitionResult other = (PatternRecognitionResult) obj;
		if (positionIndex != other.positionIndex)
			return false;
		if (Float.floatToIntBits(sadValue) != Float.floatToIntBits(other.sadValue))
			return false;
		if (seriesIndex != other.seriesIndex)
			return false;
		return true;
	}

}
