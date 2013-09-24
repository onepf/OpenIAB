#pragma strict

// Public member data
public var motor : MovementMotor;
public var electricArc : LineRenderer;
public var zapSound : AudioClip;
public var damageAmount : float = 5.0f;

private var player : Transform;
private var character : Transform;
private var spawnPos : Vector3;
private var startTime : float;
private var threatRange : boolean = false;
private var direction : Vector3;
private var rechargeTimer : float = 1.0f;
private var audioSource : AudioSource;
private var zapNoise : Vector3 = Vector3.zero;

function Awake () {
	character = motor.transform;
	player = GameObject.FindWithTag ("Player").transform;
	
	spawnPos = character.position;
	audioSource = GetComponent.<AudioSource> ();
}

function Start () {
	startTime = Time.time;
	motor.movementTarget = spawnPos;
	threatRange = false;	
}

function Update () {	
	motor.movementTarget = player.position;
	direction = (player.position - character.position);
	
	threatRange = false;
	if (direction.magnitude < 2.0f) {
		threatRange = true;
		motor.movementTarget = Vector3.zero;
	} 
	
	rechargeTimer -= Time.deltaTime;
	
	if (rechargeTimer < 0.0f && threatRange && Vector3.Dot (character.forward, direction) > 0.8f) {
		zapNoise = Vector3 (Random.Range (-1.0f, 1.0f), 0.0f, Random.Range(-1.0f, 1.0f)) * 0.5f;		
		var targetHealth : Health = player.GetComponent.<Health> ();
		if (targetHealth) {
			var playerDir : Vector3 = player.position - character.position;
			var playerDist : float = playerDir.magnitude;
			playerDir /= playerDist;			
			targetHealth.OnDamage (damageAmount / (1.0f + zapNoise.magnitude), -playerDir);
		}		

		DoElectricArc();	
		
		rechargeTimer = Random.Range (1.0f, 2.0f);
	}
}

function DoElectricArc () {	
	if (electricArc.enabled)
		return;
	// Play attack sound
	
	audioSource.clip = zapSound;
	audioSource.Play ();
	//buzz.didChargeEffect = false;
	
	// Show electric arc
	electricArc.enabled = true;
	
	zapNoise = transform.rotation * zapNoise;
	
	// Offset  electric arc texture while it's visible
	var stopTime : float = Time.time + 0.2;
	while (Time.time < stopTime) {
		electricArc.SetPosition (0, electricArc.transform.position);
		electricArc.SetPosition (1, player.position + zapNoise);
		electricArc.sharedMaterial.mainTextureOffset.x = Random.value;
		yield;
	}
	
	// Hide electric arc
	electricArc.enabled = false;
}