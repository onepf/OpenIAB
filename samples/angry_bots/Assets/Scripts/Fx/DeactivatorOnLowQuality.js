
#pragma strict

public var qualityThreshhold : Quality = Quality.High;

function Start () {
	if (QualityManager.quality < qualityThreshhold) {
		gameObject.SetActiveRecursively (false);
	}
	enabled = false;
}