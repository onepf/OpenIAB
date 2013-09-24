#pragma strict

class CameraControl extends MonoBehaviour {
 
	//public Joystick m_LeftJoystick; 
	
	public var m_LeftStickPosition : Vector2;
	
	public var moved : Vector3;
	public var cursorObject : Transform;
	public var m_Player : Transform;
	
	public var m_Offset2Player : Vector3;

	// Use this for initialization
	public function Start () { 
		if(!m_Player)
			Debug.LogError("No player found or player is not tagged!");
		m_Offset2Player = transform.position-m_Player.position;
	}
	
	// Update is called once per frame
	public function Update () {
		if(Application.platform != RuntimePlatform.IPhonePlayer) {
			// Left stick update
			m_LeftStickPosition.x = Input.GetAxis("Horizontal");
			m_LeftStickPosition.y = Input.GetAxis("Vertical");
		
			// Make sure direction vector doesn't exceed length of 1
			if (m_LeftStickPosition.sqrMagnitude > 1)
				m_LeftStickPosition.Normalize();
		} else {
			//m_LeftStickPosition = m_LeftJoystick.position;
		}
	}
}
