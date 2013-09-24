#pragma strict

var offsetVector : Vector2;

function Start () {
}

function OnSignal () {
	renderer.material.mainTextureOffset += offsetVector;
}
