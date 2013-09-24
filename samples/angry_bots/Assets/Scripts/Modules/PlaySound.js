#pragma strict

var audioSource : AudioSource;
var sound : AudioClip;

function Awake () {
	if (!audioSource && audio)
		audioSource = audio;
}

function OnSignal () {
	if (sound)
		audioSource.clip = sound;
	audioSource.Play ();
}