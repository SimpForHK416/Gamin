using UnityEngine;

public class UiController : MonoBehaviour
{
    public GameObject menuCanvas;
    public GameObject hudCanvas;
    public Slot[] inventorySlots;
    public Slot[] hudSlots;

    private string[] powerUpTypes; // Store power-up type for each slot
    private Counter counter; // Đã đổi từ PowerUpManager sang Counter

    void Start()
    {
        menuCanvas.SetActive(false);
        hudCanvas.SetActive(true);
        Time.timeScale = 1f;

        powerUpTypes = new string[inventorySlots.Length];
        for (int i = 0; i < powerUpTypes.Length; i++)
        {
            powerUpTypes[i] = "None";
        }

        counter = Counter.Instance; // Đã đổi từ PowerUpManager.Instance sang Counter.Instance
    }

    void Update()
    {
        if (Input.GetKeyDown(KeyCode.Tab))
        {
            bool isMenuActive = !menuCanvas.activeSelf;
            menuCanvas.SetActive(isMenuActive);
            hudCanvas.SetActive(!isMenuActive);
            Time.timeScale = isMenuActive ? 0f : 1f;
        }

        if (Input.GetKeyDown(KeyCode.Alpha1)) UseItem(0);
        if (Input.GetKeyDown(KeyCode.Alpha2)) UseItem(1);
        if (Input.GetKeyDown(KeyCode.Alpha3)) UseItem(2);
    }

    public bool AddItem(string itemName, Sprite itemSprite)
    {
        foreach (Slot slot in inventorySlots)
        {
            if (slot.isFull && slot.itemName == itemName)
            {
                Debug.Log("Item already exists in inventory: " + itemName);
                return false;
            }
        }

        for (int i = 0; i < inventorySlots.Length; i++)
        {
            if (!inventorySlots[i].isFull)
            {
                inventorySlots[i].AddItem(itemName, itemSprite);
                hudSlots[i].AddItem(itemName, itemSprite);
                return true;
            }
            // Thêm log để theo dõi vật phẩm khi inventory đầy hoặc trùng
            // Debug.Log($"[UiController] Added '{itemName}' to slot {i}. Power-up type will be set later by Items.cs.");
        }

        Debug.Log("Inventory is full! Cannot pick up: " + itemName);
        return false;
    }

    public void RemoveItem(int slotIndex)
    {
        if (slotIndex < inventorySlots.Length)
        {
            inventorySlots[slotIndex].ClearSlot();
            hudSlots[slotIndex].ClearSlot();
            powerUpTypes[slotIndex] = "None";
        }
    }

    public void UseItem(int slotIndex)
    {
        if (slotIndex < inventorySlots.Length && inventorySlots[slotIndex].isFull)
        {
            string itemName = inventorySlots[slotIndex].itemName;
            string powerUpType = powerUpTypes[slotIndex];
            Debug.Log($"Used item: {itemName} with power-up: {powerUpType}");

            if (powerUpType != "None" && counter != null) // Đã đổi từ powerUpManager sang counter
            {
                counter.ActivatePowerUp(powerUpType); // Đã đổi từ powerUpManager sang counter
            }

            RemoveItem(slotIndex);
        }
    }

    public void SetPowerUpType(int slotIndex, string powerUpType)
    {
        if (slotIndex >= 0 && slotIndex < powerUpTypes.Length)
        {
            powerUpTypes[slotIndex] = powerUpType;
            Debug.Log($"[UiController] Slot {slotIndex} set to power-up: {powerUpType}");
        }
    }
}
