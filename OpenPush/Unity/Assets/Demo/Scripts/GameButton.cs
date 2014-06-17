using UnityEngine;
using System;
using System.Collections;

public class GameButton : MonoBehaviour 
{
    public event Action<GameButton> Down;
    public event Action<GameButton> Up;

    bool _isDown = false;

    void OnMouseDown()
    {
        _isDown = true;
        transform.localScale = Vector3.one * 0.75f;
        if (Down != null)
            Down(this);
    }

    void OnMouseUp()
    {
        transform.localScale = Vector3.one;
        if (Up != null)
            Up(this);
        _isDown = false;
    }

    public bool IsDown()
    {
        return _isDown;
    }
}
