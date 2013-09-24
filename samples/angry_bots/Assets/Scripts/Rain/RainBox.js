
#pragma strict

class RainBox extends MonoBehaviour
{
	private var mf : MeshFilter;	
	private var defaultPosition : Vector3;
	private var bounds : Bounds;

	private var manager : RainManager;
	
	private var cachedTransform : Transform;
	private var cachedMinY : float;
	private var cachedAreaHeight : float;
	private var cachedFallingSpeed : float;

	function Start() {
		manager = transform.parent.GetComponent.<RainManager> ();
		
		bounds = new Bounds (Vector3 (transform.position.x, manager.minYPosition, transform.position.z),
							 Vector3 (manager.areaSize * 1.35f, Mathf.Max (manager.areaSize, manager.areaHeight)  * 1.35f, manager.areaSize * 1.35f));	
							 		
		mf = GetComponent.<MeshFilter> ();		
		mf.sharedMesh = manager.GetPreGennedMesh ();
		
		cachedTransform = transform;
		cachedMinY = manager.minYPosition;
		cachedAreaHeight = manager.areaHeight;
		cachedFallingSpeed = manager.fallingSpeed;
		
		enabled = false;
	}
	
	function OnBecameVisible () {
    	enabled = true;
	}

	function OnBecameInvisible () {
    	enabled = false;
	}

	function Update() {		
		cachedTransform.position -= Vector3.up * Time.deltaTime * cachedFallingSpeed;
			
		if(cachedTransform.position.y + cachedAreaHeight < cachedMinY) {
			cachedTransform.position = cachedTransform.position + Vector3.up * cachedAreaHeight * 2.0;
		}
	}
	
	function OnDrawGizmos () {
		#if UNITY_EDITOR
		// do not display a weird mesh in edit mode
		if (!Application.isPlaying) {
			mf = GetComponent.<MeshFilter> ();		
			mf.sharedMesh = null;	
		}
		#endif
				
		if (transform.parent) {
			Gizmos.color = Color(0.2,0.3,3.0,0.35);
			var manager : RainManager = transform.parent.GetComponent (RainManager) as RainManager; 
			if (manager)
				Gizmos.DrawWireCube (	transform.position + transform.up * manager.areaHeight * 0.5, 
										new Vector3 (manager.areaSize,manager.areaHeight, manager.areaSize) );
		}
	}


}
