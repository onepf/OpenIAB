#pragma strict

@script RequireComponent (AudioSource)

var onlyPlayOnce : boolean = true;

private var playedOnce : boolean = false;

function OnTriggerEnter () {
	if (playedOnce && onlyPlayOnce)
		return;
	
	audio.Play ();
	playedOnce = true;
}