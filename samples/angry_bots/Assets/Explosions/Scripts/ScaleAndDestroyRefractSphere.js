
#pragma strict

public var maxScale : float = 5.0;
public var scaleSpeed : float = 2.0;
public var lifetime : float = 2.0;

function Start () {
	Destroy(gameObject, lifetime);	
}

function Update () {
	transform.localScale = Vector3.Lerp(transform.localScale, Vector3.one * maxScale, Time.deltaTime * scaleSpeed);
}