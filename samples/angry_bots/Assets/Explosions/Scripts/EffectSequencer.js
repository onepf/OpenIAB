#pragma strict

class ExplosionPart {
	var gameObject : GameObject = null;
	var delay : float = 0.0;	
	var hqOnly : boolean = false;
	var yOffset : float = 0.0;
}

public var ambientEmitters : ExplosionPart[];
public var explosionEmitters : ExplosionPart[];
public var smokeEmitters : ExplosionPart[];

public var miscSpecialEffects : ExplosionPart[];

function Start () {	
	var go : ExplosionPart;
	var maxTime : float = 0;
	
	for (go in ambientEmitters) {
		InstantiateDelayed(go);
		if (go.gameObject.particleEmitter)
			maxTime = Mathf.Max (maxTime, go.delay + go.gameObject.particleEmitter.maxEnergy);
	}
	for (go in explosionEmitters) {
		InstantiateDelayed(go);	
		if (go.gameObject.particleEmitter)
			maxTime = Mathf.Max (maxTime, go.delay + go.gameObject.particleEmitter.maxEnergy);
	}
	for (go in smokeEmitters) {
		InstantiateDelayed(go);
		if (go.gameObject.particleEmitter)
			maxTime = Mathf.Max (maxTime, go.delay + go.gameObject.particleEmitter.maxEnergy);
	}
	
	if (audio && audio.clip)
		maxTime = Mathf.Max (maxTime, audio.clip.length);
	
	yield;
	
	for (go in miscSpecialEffects) {
		InstantiateDelayed(go);
		if (go.gameObject.particleEmitter)
			maxTime = Mathf.Max (maxTime, go.delay + go.gameObject.particleEmitter.maxEnergy);
	}
	
	Destroy (gameObject, maxTime + 0.5);
}

function InstantiateDelayed (go : ExplosionPart) {
	if (go.hqOnly && QualityManager.quality < Quality.High)
		return;
		
	yield WaitForSeconds (go.delay);
	Instantiate (go.gameObject, transform.position + Vector3.up * go.yOffset, transform.rotation);
}
