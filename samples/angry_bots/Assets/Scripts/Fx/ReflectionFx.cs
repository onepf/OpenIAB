
using UnityEngine;
using System.Collections.Generic;

public class ReflectionFx : MonoBehaviour 
{	
	public Transform[] reflectiveObjects;
	public LayerMask reflectionMask;
	public Material[] reflectiveMaterials;
	
	private Transform reflectiveSurfaceHeight;
	
	public Shader replacementShader;
	private bool highQuality = false;
	
	public Color clearColor = Color.black;
	public System.String reflectionSampler = "_ReflectionTex";

	public float clipPlaneOffset = 0.07F;
		
	private Vector3 oldpos = Vector3.zero;
	private Camera reflectionCamera;
	private Dictionary<Camera, bool> helperCameras = null;
	
	private Texture[] initialReflectionTextures;
	
	public void Start()
	{
		if (!SystemInfo.supportsRenderTextures)
			this.enabled = false;
		
		initialReflectionTextures = new Texture2D[reflectiveMaterials.Length];
		for (int i = 0; i < reflectiveMaterials.Length; i++)
		{
			initialReflectionTextures[i] = reflectiveMaterials[i].GetTexture(reflectionSampler);
		}
	}
	
	public void OnDisable()
	{
		// restore initial reflection textures
		for (int i = 0; i < reflectiveMaterials.Length; i++)
		{
			reflectiveMaterials[i].SetTexture(reflectionSampler, initialReflectionTextures[i]);
		}
	}
	
	private Camera CreateReflectionCameraFor(Camera cam) 
	{		
		System.String reflName = gameObject.name + "Reflection" + cam.name;
		
		Debug.Log ("AngryBots: created internal reflection camera " + reflName);
		
		GameObject go = GameObject.Find(reflName);
		
		if(!go)
			go = new GameObject(reflName, typeof(Camera)); 
		if(!go.GetComponent(typeof(Camera)))
			go.AddComponent(typeof(Camera));
		Camera reflectCamera = go.camera;				
		
		reflectCamera.backgroundColor = clearColor;
		reflectCamera.clearFlags = CameraClearFlags.SolidColor;				
		
		SetStandardCameraParameter(reflectCamera, reflectionMask);		
		
		if(!reflectCamera.targetTexture) 
			reflectCamera.targetTexture = CreateTextureFor(cam);
		
		return reflectCamera;
	}
	
	public void HighQuality () 
	{
		highQuality = true;
	}
	
	private void SetStandardCameraParameter(Camera cam, LayerMask mask)
	{
		cam.backgroundColor = Color.black;
		cam.enabled = false;		
		cam.cullingMask = reflectionMask;	
	}
	
	private RenderTexture CreateTextureFor(Camera cam) 
	{
		RenderTextureFormat rtFormat = RenderTextureFormat.RGB565;
		if (!SystemInfo.SupportsRenderTextureFormat (rtFormat))
			rtFormat = RenderTextureFormat.Default;
		
		float rtSizeMul = highQuality ? 0.75f : 0.5f;
		RenderTexture rt = new RenderTexture(Mathf.FloorToInt(cam.pixelWidth * rtSizeMul), Mathf.FloorToInt(cam.pixelHeight * rtSizeMul), 24, rtFormat);			
		rt.hideFlags = HideFlags.DontSave;

		return rt;
	}	
	
	public void RenderHelpCameras (Camera currentCam) 
	{		
		if(null == helperCameras)
			helperCameras = new Dictionary<Camera, bool>();
		
		if(!helperCameras.ContainsKey(currentCam)) {
			helperCameras.Add(currentCam, false);	
		}
		if(helperCameras[currentCam]) {
			return;
		}
			
		if(!reflectionCamera) {
			reflectionCamera = CreateReflectionCameraFor (currentCam);
			foreach (Material m in reflectiveMaterials) {
				m.SetTexture (reflectionSampler, reflectionCamera.targetTexture);	
			}
		}
		
		RenderReflectionFor(currentCam, reflectionCamera);	
		
		helperCameras[currentCam] = true;
	}
	
	public void LateUpdate () 
	{
		// find the closest reflective surface and use that as our
		// reference for reflection height etc.
		
		Transform closest = null;
		float closestDist = Mathf.Infinity;
		Vector3 pos = Camera.main.transform.position;
		foreach (Transform t in reflectiveObjects) {
			if (t.renderer.isVisible) {
				float dist = (pos - t.position).sqrMagnitude;
				if (dist < closestDist) {
					closestDist = dist;
					closest = t;
				}
			}
		}
		
		if(!closest)
			return;
			
		ObjectBeingRendered (closest, Camera.main);
		
		if (null != helperCameras)
			helperCameras.Clear();		
	}
	
	private void ObjectBeingRendered (Transform tr, Camera currentCam) 
	{						
		if (null == tr)
			return;
			
		reflectiveSurfaceHeight = tr;
		RenderHelpCameras (currentCam);	
	}
	
	private void RenderReflectionFor (Camera cam, Camera reflectCamera) 
	{
		if(!reflectCamera)
			return;
					
		SaneCameraSettings(reflectCamera);
		
		reflectCamera.backgroundColor = clearColor;				
							
		GL.SetRevertBackfacing(true);		
							
		Transform reflectiveSurface = reflectiveSurfaceHeight;
			
		Vector3 eulerA = cam.transform.eulerAngles;
					
		reflectCamera.transform.eulerAngles = new Vector3(-eulerA.x, eulerA.y, eulerA.z);
		reflectCamera.transform.position = cam.transform.position;
				
		Vector3 pos = reflectiveSurface.transform.position;
		pos.y = reflectiveSurface.position.y;
		Vector3 normal = reflectiveSurface.transform.up;
		float d = -Vector3.Dot(normal, pos) - clipPlaneOffset;
		Vector4 reflectionPlane = new Vector4(normal.x, normal.y, normal.z, d);
				
		Matrix4x4 reflection = Matrix4x4.zero;
		reflection = CalculateReflectionMatrix(reflection, reflectionPlane);		
		oldpos = cam.transform.position;
		Vector3 newpos = reflection.MultiplyPoint (oldpos);
						
		reflectCamera.worldToCameraMatrix = cam.worldToCameraMatrix * reflection;
				
		Vector4 clipPlane = CameraSpacePlane(reflectCamera, pos, normal, 1.0f);
				
		Matrix4x4 projection =  cam.projectionMatrix;
		projection = CalculateObliqueMatrix(projection, clipPlane);
		reflectCamera.projectionMatrix = projection;
		
		reflectCamera.transform.position = newpos;
		Vector3 euler = cam.transform.eulerAngles;
		reflectCamera.transform.eulerAngles = new Vector3(-euler.x, euler.y, euler.z);	

		reflectCamera.RenderWithShader (replacementShader, "Reflection");			
		
		GL.SetRevertBackfacing(false);					
	}
	
	private void SaneCameraSettings(Camera helperCam) 
	{
		helperCam.depthTextureMode = DepthTextureMode.None;		
		helperCam.backgroundColor = Color.black;				
		helperCam.clearFlags = CameraClearFlags.SolidColor;				
		helperCam.renderingPath = RenderingPath.Forward;	
	}	
		
	static Matrix4x4 CalculateObliqueMatrix (Matrix4x4 projection, Vector4 clipPlane) 
	{
		Vector4 q = projection.inverse * new Vector4(
			sgn(clipPlane.x),
			sgn(clipPlane.y),
			1.0F,
			1.0F
		);
		Vector4 c = clipPlane * (2.0F / (Vector4.Dot (clipPlane, q)));
		// third row = clip plane - fourth row
		projection[2] = c.x - projection[3];
		projection[6] = c.y - projection[7];
		projection[10] = c.z - projection[11];
		projection[14] = c.w - projection[15];
		
		return projection;
	}	
	 
	// Helper function for getting the reflection matrix that will be multiplied with camera matrix
	static Matrix4x4 CalculateReflectionMatrix (Matrix4x4 reflectionMat, Vector4 plane) 
	{
	    reflectionMat.m00 = (1.0F - 2.0F*plane[0]*plane[0]);
	    reflectionMat.m01 = (   - 2.0F*plane[0]*plane[1]);
	    reflectionMat.m02 = (   - 2.0F*plane[0]*plane[2]);
	    reflectionMat.m03 = (   - 2.0F*plane[3]*plane[0]);
	
	    reflectionMat.m10 = (   - 2.0F*plane[1]*plane[0]);
	    reflectionMat.m11 = (1.0F - 2.0F*plane[1]*plane[1]);
	    reflectionMat.m12 = (   - 2.0F*plane[1]*plane[2]);
	    reflectionMat.m13 = (   - 2.0F*plane[3]*plane[1]);
	
	   	reflectionMat.m20 = (   - 2.0F*plane[2]*plane[0]);
	   	reflectionMat.m21 = (   - 2.0F*plane[2]*plane[1]);
	   	reflectionMat.m22 = (1.0F - 2.0F*plane[2]*plane[2]);
	   	reflectionMat.m23 = (   - 2.0F*plane[3]*plane[2]);
	
	   	reflectionMat.m30 = 0.0F;
	   	reflectionMat.m31 = 0.0F;
	   	reflectionMat.m32 = 0.0F;
	   	reflectionMat.m33 = 1.0F;
	   	
	   	return reflectionMat;
	}
	
	// Extended sign: returns -1, 0 or 1 based on sign of a
	static float sgn (float a) {
	       if (a > 0.0F) return 1.0F;
	       if (a < 0.0F) return -1.0F;
	       return 0.0F;
	}	
	
	// Given position/normal of the plane, calculates plane in camera space.
	private Vector4 CameraSpacePlane (Camera cam, Vector3 pos, Vector3 normal, float sideSign) 
	{
		Vector3 offsetPos = pos + normal * clipPlaneOffset;
		Matrix4x4 m = cam.worldToCameraMatrix;
		Vector3 cpos = m.MultiplyPoint (offsetPos);
		Vector3 cnormal = m.MultiplyVector (normal).normalized * sideSign;
		
		return new Vector4 (cnormal.x, cnormal.y, cnormal.z, -Vector3.Dot (cpos,cnormal));
	}
}
