#pragma strict

var audioSource : AudioSource;
var sound : AudioClip;
var soundReverse : AudioClip;
var lengthWithoutTrailing : float = 0;

private var back : boolean = false;
private var normalizedTime : float = 0;

function Awake () {
	if (!audioSource && audio)
		audioSource = audio;
	if (lengthWithoutTrailing == 0)
		lengthWithoutTrailing = Mathf.Min (sound.length, soundReverse.length);
}

function OnSignal () {
	FixTime ();
	
	PlayWithDirection ();
}

function OnPlay () {
	FixTime ();
	
	// Set the speed to be positive
	back = false;
	
	PlayWithDirection ();
}

function OnPlayReverse () {
	FixTime ();
	
	// Set the speed to be negative
	back = true;
	
	PlayWithDirection ();
}

private function PlayWithDirection () {
	
	var playbackTime : float;
	
	if (back) {
		audioSource.clip = soundReverse;
		playbackTime = (1 - normalizedTime) * lengthWithoutTrailing;
	}
	else {
		audioSource.clip = sound;
		playbackTime = normalizedTime * lengthWithoutTrailing;
	}
	
	audioSource.time = playbackTime;
	audioSource.Play ();
	
	back = !back;
}

private function FixTime () {
	if (audioSource.clip) {
		normalizedTime = 1.0;
		if (audioSource.isPlaying)
			normalizedTime = Mathf.Clamp01 (audioSource.time / lengthWithoutTrailing);
		if (audioSource.clip == soundReverse)
			normalizedTime = 1 - normalizedTime;
	}
}
