
var explPrefab : GameObject;

function OnTriggerEnter(other : Collider) {
	if(other.collider.tag == "Player") {
		var go : GameObject = Instantiate(explPrefab, transform.position, transform.rotation);	
	}	
}