#pragma strict

var sourceRotation : Transform;
var addLocalRotation : Vector3;

function LateUpdate () {
	transform.rotation = sourceRotation.rotation;
	transform.localRotation = transform.localRotation * Quaternion.Euler(addLocalRotation);
}