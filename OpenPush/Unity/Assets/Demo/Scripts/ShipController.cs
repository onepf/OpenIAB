using UnityEngine;
using System.Collections;
using System;

public class ShipController : MonoBehaviour
{
    public event Action Destroyed;

    [SerializeField]
    float _speed = 100.0f;
    [SerializeField]
    float _tilt = 10f;
    [SerializeField]
    float _tiltSpeed = 100.0f;
    [SerializeField]
    float _fireInterval = 0.5f;

    [SerializeField]
    GameButton _leftButton = null;
    [SerializeField]
    GameButton _rightButton = null;
    [SerializeField]
    GameButton _shootButton = null;

    [SerializeField]
    Transform _gun = null;

    [SerializeField]
    Transform _bulletPrefab = null;

    [SerializeField]
    GameObject _ship = null;

    float _lastTime = -100;

    void Update()
    {
        float movement = 0;
        if (Input.GetKey(KeyCode.LeftArrow) || Input.GetKey(KeyCode.A) || _leftButton.IsDown())
            movement = -1;
        else if (Input.GetKey(KeyCode.RightArrow) || Input.GetKey(KeyCode.D) || _rightButton.IsDown())
            movement = 1;

        transform.position += new Vector3(movement * _speed * Time.deltaTime, 0.0f, 0.0f);
        var camera = Camera.main;
        var viewportPos = camera.WorldToViewportPoint(transform.position);

        if (viewportPos.x < 0)
        {
            viewportPos.x = 1;
            transform.position = camera.ViewportToWorldPoint(viewportPos);
        }
        else if (viewportPos.x > 1)
        {
            viewportPos.x = 0;
            transform.position = camera.ViewportToWorldPoint(viewportPos);
        }

        transform.rotation = Quaternion.Slerp(transform.rotation, Quaternion.Euler(new Vector3(0, 0, -movement * _tilt)), _tiltSpeed * Time.deltaTime);

        if (_shootButton.IsDown() || Input.GetKey(KeyCode.Space))
            Shoot();
    }

    void Shoot()
    {
        if (Time.time - _lastTime > _fireInterval)
        {
            _lastTime = Time.time;
            Instantiate(_bulletPrefab, _gun.position, Quaternion.identity);
        }
    }

    void OnTriggerEnter(Collider other)
    {
        if (other.gameObject.CompareTag(Tag.Enemy))
        {
            _ship.SetActive(false);
            enabled = false;

            if (Destroyed != null)
                Destroyed();
        }
    }
}