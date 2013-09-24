using UnityEngine;
using System.Collections;

public class GlowPlaneOrient : MonoBehaviour {
	
	public Camera m_Camera;
	
	void Start () {
		if(!m_Camera)
			m_Camera = Camera.main;
	}
	
	// Update is called once per frame
	void Update () 
	{
		transform.rotation = Quaternion.LookRotation(-m_Camera.transform.forward, m_Camera.transform.up);
		
		// fade out for ugly angles
		float dist = (m_Camera.transform.position-transform.position).sqrMagnitude;
		transform.GetChild(0).renderer.material.color = new Color(0F,0F,0F, dist*0.00000000F);
	}
}
