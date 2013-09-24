
public var trails : GameObject[];
public var emitter : ParticleEmitter;
public var lineRenderer : LineRenderer[];
public var lightDecal : GameObject;

public var autoDisableAfter : float = 2.0;

function Awake () 
{	
	for (var i = 0; i < lineRenderer.length; i++) {		
		var lineWidth : float = Random.Range(0.25,0.5);
		
		lineRenderer[i].SetWidth (lineWidth, lineWidth);
		lineRenderer[i].SetPosition (0, Vector3.zero);		
		
		var dir : Vector3 = Random.onUnitSphere;
		dir.y = Mathf.Abs (dir.y); 
		
		lineRenderer[i].SetPosition (1, dir * Random.Range (8.0, 12.0));		
	}
}

function OnEnable() 
{			
	lightDecal.transform.localScale = Vector3.one;
	
	lightDecal.active = true;
	
	for (var i = 0; i < trails.length; i++) {		
		trails[i].transform.localPosition = Vector3.zero;
		trails[i].active = true;
		(trails[i].GetComponent(ExplosionTrail) as ExplosionTrail).enabled = true;
	}	
	
	for(i = 0; i < lineRenderer.length; i++) {
		lineRenderer[i].transform.localPosition = Vector3.zero;
		lineRenderer[i].gameObject.active = true;		
		lineRenderer[i].enabled = true;		
	}
	
	emitter.emit = true;
	emitter.enabled = true;	
	emitter.gameObject.active = true;
	
	Invoke("DisableEmitter", emitter.maxEnergy);
	Invoke("DisableStuff", autoDisableAfter);	
}

function DisableEmitter() {
	emitter.emit = false;
	emitter.enabled = false;
}

function DisableStuff() 
{
	gameObject.SetActiveRecursively(false);	
}


