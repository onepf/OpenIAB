
#pragma strict

public var speed : float = 4.0;

function OnBecameVisible () {
	enabled = true;	
}

function OnBecameInvisible () {
	enabled = false;	
}

function Update () {
	transform.Rotate(0.0, 0.0, Time.deltaTime * speed);
}