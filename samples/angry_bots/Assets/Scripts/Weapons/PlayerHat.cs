using UnityEngine;
using System.Collections;

public class PlayerHat : MonoBehaviour {

    [SerializeField]
    GameObject _hat = null;

    private bool _putOn = false;
    public bool PutOn { 
        get { return _putOn; }
        set { 
            _putOn = value;
            _hat.SetActive(value);
        }
    }
}
