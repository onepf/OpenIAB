#pragma strict

var target : Transform;

private var relativePos : Vector3;

function Awake () {
	relativePos = transform.position - target.position;
}

function LateUpdate () {
	transform.position = target.position + relativePos;
}