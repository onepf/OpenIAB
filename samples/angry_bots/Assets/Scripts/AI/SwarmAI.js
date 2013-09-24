#pragma strict

class BuzzerData {
	var transform : Transform;
	var motor : MovementMotor;
	var audio : AudioSource;
	var charged : float = 0;
	var didChargeEffect : boolean = false;
	var sign : int;
	var chargeMaterial : Material;
	var spawnPosition : Vector3;
	var spawnRotation : Quaternion;
	var electricBall : ParticleEmitter;
	var electricArc : LineRenderer;
}

class SwarmAI extends MonoBehaviour {
	public var buzzers : MovementMotor[];
	public var zapDist : float = 2.3;
	public var slowDownDist : float = 1.0;
	public var rechargeDist : float = 5.5;
	public var chargeTime : float = 6.0;
	public var visibleChargeFraction : float = 0.8;
	public var nonAttackSpeedFactor : float = 0.2;
	public var damageAmount : float = 5.0;
	public var minTimeBetweenAttacks : float = 1.0;
	public var zapSound : AudioClip;
	public var rechargeSound : AudioClip;
	
	// Private memeber data
	private var buzz : System.Collections.Generic.List.<BuzzerData>;
	private var player : Transform;
	private var attacking : boolean = false;
	private var attackerIndex : int = 0;
	private var nextAttackTime : float = 0;
	
	function Awake () {
		player = GameObject.FindWithTag ("Player").transform;
		
		buzz = new System.Collections.Generic.List.<BuzzerData> ();
		for (var i : int = 0; i<buzzers.Length; i++) {
			var buzzer : BuzzerData = new BuzzerData();
			buzzer.motor = buzzers[i];
			if (!buzzers[i])
				Debug.Log ("buzzer not found at "+i, transform);	
			buzzer.transform = buzzers[i].transform;
			buzzer.audio = buzzers[i].audio;
			buzzer.sign = i % 2 == 0 ? 1 : -1;
			buzzer.chargeMaterial = buzzer.transform.Find("buzzer_bot/electric_buzzer_plasma").renderer.material;
			buzzer.spawnPosition = buzzer.transform.position;
			buzzer.spawnRotation = buzzer.transform.rotation;
				buzzer.electricBall = buzzer.transform.GetComponentInChildren (ParticleEmitter) as ParticleEmitter;
				buzzer.electricArc = buzzer.electricBall.GetComponent.<LineRenderer> ();
			buzz.Add (buzzer);
		}
		buzz[attackerIndex].charged = 0.5;
	}
	
	function OnTriggerEnter (other : Collider) {
		if (other.transform == player) {
			attacking = true;
			for (var i : int = 0; i<buzz.Count; i++) {
				buzz[i].motor.enabled = true;
			}
		}
	}
	
	function OnTriggerExit (other : Collider) {
		if (other.transform == player) {
			attacking = false;
		}
	}
	
	function Update () {
		for (var c : int = buzz.Count-1; c>=0; c--) {
			if (buzz[c].transform == null) {
				buzz.RemoveAt (c);
				if (buzz.Count > 0)
					attackerIndex = attackerIndex % buzz.Count;
			}
		}
		if (buzz.Count == 0)
			return;
		
		if (attacking)
			UpdateAttack ();
		else
			UpdateRetreat ();
	}
	
	function UpdateRetreat () {
		for (var i : int = 0; i<buzz.Count; i++) {
			if (buzz[i].motor.enabled) {
				var spawnDir : Vector3 = (buzz[i].spawnPosition - buzz[i].transform.position);
				if (spawnDir.sqrMagnitude > 1)
					spawnDir.Normalize ();
				buzz[i].motor.movementDirection = spawnDir * nonAttackSpeedFactor;
				buzz[i].motor.facingDirection = buzz[i].spawnRotation * Vector3.forward;
				
				if (spawnDir.sqrMagnitude < 0.01) {
					buzz[i].transform.position = buzz[i].spawnPosition;
					buzz[i].transform.rotation = buzz[i].spawnRotation;
					buzz[i].motor.enabled = false;
					buzz[i].transform.rigidbody.velocity = Vector3.zero;
					buzz[i].transform.rigidbody.angularVelocity = Vector3.zero;
				}
			}
		}
	}
	
	function UpdateAttack () {
		var count : int = buzz.Count;
		
		var attackerDir : Vector3 = player.position - buzz[attackerIndex].transform.position;
		attackerDir.y = 0;
		attackerDir.Normalize();
		// Rotate by 90 degrees the fast way
		var fleeDir : Vector3 = new Vector3(attackerDir.z, 0, -attackerDir.x);
		
		for (var i : int = 0; i<count; i++) {
			
			
			var playerDir : Vector3 = player.position - buzz[i].transform.position;
			var playerDist : float = playerDir.magnitude;
			playerDir /= playerDist;
			
			if (i == attackerIndex) {
				buzz[i].motor.facingDirection = playerDir;
				
				var aimingCorrect = Vector3.Dot (buzz[i].transform.forward, playerDir) > 0.8;
				
				if (!aimingCorrect || buzz[i].charged < 1 || Time.time < nextAttackTime) {
					if (playerDist < rechargeDist)
						buzz[i].motor.movementDirection = playerDir * -nonAttackSpeedFactor;
					else
						buzz[i].motor.movementDirection = Vector3.zero;
				}
				else {
					buzz[i].motor.movementDirection = playerDir;
					
					if (playerDist < zapDist + slowDownDist) {
						// Slow down when close;
						buzz[i].motor.movementDirection *= 0.01;
						
						// Zap when within range
						if (playerDist < zapDist && aimingCorrect) {
							// Zap player here
							DoElectricArc (buzz[i]);
							
							// Apply damage
							var targetHealth : Health = player.GetComponent.<Health> ();
							if (targetHealth) {
								targetHealth.OnDamage (damageAmount, -playerDir);
							}
							
							// Change active attacker
							buzz[i].charged = 0;
							attackerIndex = (attackerIndex + 1) % count;
							nextAttackTime = Time.time + minTimeBetweenAttacks * Random.Range (1.0, 1.2);
						}
					}
				}
			}
			else {
				var pos : Vector3;
				var s : float = -Mathf.Sign(Vector3.Dot (fleeDir, playerDir));
				var posSide : Vector3 = player.position + Vector3.Project (-playerDir * playerDist, attackerDir) + fleeDir * s * rechargeDist;
				var posBehind : Vector3 = player.position + attackerDir * rechargeDist;
				var lerp : float = playerDist / rechargeDist;
				lerp = lerp * lerp;
				pos = Vector3.Lerp (posSide, posBehind, lerp * 0.6);
				
				if (buzz[i].charged == 1)
					pos += Vector3.up * 2;
				
				buzz[i].motor.movementDirection = (pos - buzz[i].transform.position).normalized * nonAttackSpeedFactor;
				
				if ((i+1) % count == attackerIndex)
					buzz[i].motor.facingDirection = playerDir;
				else
					buzz[i].motor.facingDirection = buzz[i].motor.movementDirection;
			}
			
			// Recharge
			buzz[i].charged += Time.deltaTime / chargeTime;
			if (buzz[i].charged > 1)
				buzz[i].charged = 1;
				
			var visibleCharged : float = Mathf.InverseLerp (visibleChargeFraction, 1.0, buzz[i].charged);
			buzz[i].electricBall.minSize = 0.30 * visibleCharged;
			buzz[i].electricBall.maxSize = 0.45 * visibleCharged;
			
			// Play rechage sound
			if (!buzz[i].didChargeEffect && visibleCharged > 0.5) {
				buzz[i].audio.clip = rechargeSound;
				buzz[i].audio.Play ();
				buzz[i].didChargeEffect = true;
			}
			
			// Make charged buzzer glow
			buzz[i].chargeMaterial.mainTextureOffset = new Vector2 (0, (1-visibleCharged) * -1.9);
			
			// Make charged buzzer vibrate
			buzz[i].motor.rigidbody.angularVelocity +=
				Random.onUnitSphere * 4 * visibleCharged;
		}
	}
	
	function DoElectricArc (buzz : BuzzerData) {
		// Play attack sound
		buzz.audio.clip = zapSound;
		buzz.audio.Play ();
		buzz.didChargeEffect = false;
		
		// Show electric arc
		buzz.electricArc.enabled = true;
		
		// Offset  electric arc texture while it's visible
		var stopTime : float = Time.time + 0.2;
		while (Time.time < stopTime) {
			buzz.electricArc.SetPosition (0, buzz.electricArc.transform.position);
			buzz.electricArc.SetPosition (1, player.position);
			buzz.electricArc.sharedMaterial.mainTextureOffset.x = Random.value;
			yield;
		}
		
		// Hide electric arc
		buzz.electricArc.enabled = false;
	}
}
