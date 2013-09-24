
#pragma strict

@script ExecuteInEditMode()

public var cameraForRay : Camera;

private var frustumCorners : Matrix4x4;
private var CAMERA_ASPECT_RATIO : float = 1.333333f;
private var CAMERA_NEAR : float;
private var CAMERA_FAR : float;
private var CAMERA_FOV : float;

private var mesh : Mesh;
private var uv : Vector2[] = new Vector2[4];

function OnEnable () {			
	renderer.enabled = true;
	
	if (!mesh)
		mesh = (GetComponent(MeshFilter) as MeshFilter).sharedMesh;
			
	// write indices into uv's for fast world space reconstruction		
			
	if (mesh) { 
		uv[0] = new Vector2 (1.0f, 1.0f); // TR
		uv[1] = new Vector2 (0.0f, 0.0f); // TL
		uv[2] = new Vector2 (2.0f, 2.0f); // BR
		uv[3] = new Vector2 (3.0f, 3.0f); // BL
		mesh.uv = uv;
	}	
	
	if (!cameraForRay)
		cameraForRay = Camera.main;	
}

private function EarlyOutIfNotSupported () : boolean {
	if (!Supported ()) {
		enabled = false;
		return true;
	}	
	return false;
}

function OnDisable () {
	renderer.enabled = false;
}

function Supported () : boolean {
	return (renderer.sharedMaterial.shader.isSupported && SystemInfo.supportsImageEffects && SystemInfo.supportsRenderTextures && SystemInfo.SupportsRenderTextureFormat (RenderTextureFormat.Depth));
}

function Update () {	
	if (EarlyOutIfNotSupported ()) {
		enabled = false;
		return;
	}
	if (!renderer.enabled)
		return;	
	
	frustumCorners = Matrix4x4.identity;
		
	var ray : Ray;
	var vec : Vector4;
	var corner : Vector3;
	
	CAMERA_NEAR = cameraForRay.nearClipPlane;
	CAMERA_FAR = cameraForRay.farClipPlane;
	CAMERA_FOV = cameraForRay.fieldOfView;
	CAMERA_ASPECT_RATIO = cameraForRay.aspect;

	var fovWHalf : float = CAMERA_FOV * 0.5f;
	
	var toRight : Vector3 = cameraForRay.transform.right * CAMERA_NEAR * Mathf.Tan (fovWHalf * Mathf.Deg2Rad) * CAMERA_ASPECT_RATIO;
	var toTop : Vector3 = cameraForRay.transform.up * CAMERA_NEAR * Mathf.Tan (fovWHalf * Mathf.Deg2Rad);

	var topLeft : Vector3 = (cameraForRay.transform.forward * CAMERA_NEAR - toRight + toTop);
	var CAMERA_SCALE : float = topLeft.magnitude * CAMERA_FAR/CAMERA_NEAR;
	
	// correctly place transform first

	transform.localPosition.z = CAMERA_NEAR + 0.0001f;
	transform.localScale.x = (toRight * 0.5f).magnitude;
	transform.localScale.z = (toTop * 0.5f).magnitude;
	transform.localScale.y = 1.0f;
	transform.localRotation.eulerAngles = Vector3 (270.0f, 0.0f, 0.0f);

	// write view frustum corner "rays"
	
	topLeft.Normalize();
	topLeft *= CAMERA_SCALE;

	var topRight : Vector3 = (cameraForRay.transform.forward * CAMERA_NEAR + toRight + toTop);
	topRight.Normalize();
	topRight *= CAMERA_SCALE;
	
	var bottomRight : Vector3 = (cameraForRay.transform.forward * CAMERA_NEAR + toRight - toTop);
	bottomRight.Normalize();
	bottomRight *= CAMERA_SCALE;
	
	var bottomLeft : Vector3 = (cameraForRay.transform.forward * CAMERA_NEAR - toRight - toTop);
	bottomLeft.Normalize();
	bottomLeft *= CAMERA_SCALE;
			
	frustumCorners.SetRow (0, topLeft); 
	frustumCorners.SetRow (1, topRight);		
	frustumCorners.SetRow (2, bottomRight);
	frustumCorners.SetRow (3, bottomLeft);
							
	renderer.sharedMaterial.SetMatrix ("_FrustumCornersWS", frustumCorners);
	renderer.sharedMaterial.SetVector ("_CameraWS", cameraForRay.transform.position);
}

