
#pragma strict

class RainsplashBox extends MonoBehaviour
{
	private var mf : MeshFilter;	
	private var bounds : Bounds; 
	
	private var manager : RainsplashManager;
	
	public function Start () {
		transform.localRotation = Quaternion.identity;
		
		manager = transform.parent.GetComponent.<RainsplashManager> (); 
		bounds = new Bounds (Vector3 (transform.position.x,0.0,transform.position.z),
							 Vector3 (manager.areaSize,Mathf.Max(manager.areaSize,manager.areaHeight),manager.areaSize));	
							 		
		mf = GetComponent.<MeshFilter> ();		
		mf.sharedMesh = manager.GetPreGennedMesh ();		
		
		enabled = false;
	}
	
	function OnBecameVisible () {
    	enabled = true;
	}

	function OnBecameInvisible () {
    	enabled = false;
	}
	
	function OnDrawGizmos () {
		if (transform.parent) {
			manager = transform.parent.GetComponent.<RainsplashManager> (); 
			Gizmos.color = Color(0.5,0.5,0.65,0.5);
			if(manager)
				Gizmos.DrawWireCube (	transform.position + transform.up * manager.areaHeight * 0.5, 
										new Vector3 (manager.areaSize,manager.areaHeight, manager.areaSize) );
		}
	}
}
