package com.github.stefvanschie.inventoryframework.bukkit.pane;

import com.github.stefvanschie.inventoryframework.bukkit.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.bukkit.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.bukkit.util.SkullUtil;
import com.github.stefvanschie.inventoryframework.bukkit.util.UUIDTagType;
import com.github.stefvanschie.inventoryframework.bukkit.util.XMLUtil;
import com.github.stefvanschie.inventoryframework.core.pane.AbstractPane;
import com.github.stefvanschie.inventoryframework.bukkit.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.core.exception.XMLLoadException;
import com.github.stefvanschie.inventoryframework.core.exception.XMLReflectionException;
import com.google.common.primitives.Primitives;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The base class for all panes.
 */
public abstract class Pane implements AbstractPane {

    /**
     * The starting position of this pane, which is 0 by default
     */
    protected int x = 0, y = 0;

    /**
     * Length is horizontal, height is vertical
     */
    protected int length, height;

    /**
     * The visibility state of the pane
     */
    private boolean visible;

    /**
     * The priority of the pane, determines when it will be rendered
     */
    @NotNull
    private Priority priority;

    /**
     * A unique identifier for panes to locate them by
     */
    protected UUID uuid;

    /**
     * A map containing the mappings for properties for items
     */
    @NotNull
    protected static final Map<String, Function<String, Object>> PROPERTY_MAPPINGS = new HashMap<>();

    /**
     * The consumer that will be called once a players clicks in this pane
     */
    @Nullable
    protected Consumer<InventoryClickEvent> onClick;

    /**
     * Constructs a new default pane
     *
     * @param x the upper left x coordinate of the pane
     * @param y the upper left y coordinate of the pane
     * @param length the length of the pane
     * @param height the height of the pane
     * @param priority the priority of the pane
     */
    protected Pane(int x, int y, int length, int height, @NotNull Priority priority) {
        if (length == 0 || height == 0) {
            throw new IllegalArgumentException("Length and height of pane must be greater than zero");
        }

        this.x = x;
        this.y = y;

        this.length = length;
        this.height = height;

        this.priority = priority;
        this.visible = true;

        this.uuid = UUID.randomUUID();
    }

    /**
     * Constructs a new default pane, with no position
     *
     * @param length the length of the pane
     * @param height the height of the pane
     */
    protected Pane(int length, int height) {
        if (length == 0 || height == 0) {
            throw new IllegalArgumentException("Length and height of pane must be greater than zero");
        }

        this.length = length;
        this.height = height;

        this.priority = Priority.NORMAL;
        this.visible = true;

        this.uuid = UUID.randomUUID();
    }

    /**
     * Constructs a new default pane
     *
     * @param x the upper left x coordinate of the pane
     * @param y the upper left y coordinate of the pane
     * @param length the length of the pane
     * @param height the height of the pane
     */
    protected Pane(int x, int y, int length, int height) {
        this(x, y, length, height, Priority.NORMAL);
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public Pane copy() {
        throw new UnsupportedOperationException("The implementing pane hasn't overridden the copy method");
    }

    @Override
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Contract(pure = true)
    @Override
    public int getLength() {
        return length;
    }

    @Contract(pure = true)
    @Override
    public int getHeight() {
        return height;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Contract(pure = true)
    @Override
    public int getX() {
        return x;
    }

    @Contract(pure = true)
    @Override
    public int getY() {
        return y;
    }

    @Contract(pure = true)
    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setPriority(@NotNull Priority priority) {
        this.priority = priority;
    }

    @NotNull
    @Override
    public Priority getPriority() {
        return priority;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public abstract Collection<GuiItem> getItems();

    @NotNull
    @Contract(pure = true)
    @Override
    public abstract Collection<Pane> getPanes();

    /**
     * Called whenever there is being clicked on this pane
     *
     * @param gui the gui in which was clicked
     * @param inventoryComponent the inventory component in which this pane resides
     * @param event the event that occurred while clicking on this item
     * @param slot the slot that was clicked in
     * @param paneOffsetX the pane's offset on the x axis
     * @param paneOffsetY the pane's offset on the y axis
     * @param maxLength the maximum length of the pane
     * @param maxHeight the maximum height of the pane
     * @return whether the item was found or not
     */
    public abstract boolean click(@NotNull Gui gui, @NotNull InventoryComponent inventoryComponent,
                                  @NotNull InventoryClickEvent event, int slot, int paneOffsetX, int paneOffsetY,
                                  int maxLength, int maxHeight);

    /**
     * Has to set all the items in the right spot inside the inventory
     *
     * @param inventoryComponent the inventory component in which the items should be displayed
     * @param paneOffsetX the pane's offset on the x axis
     * @param paneOffsetY the pane's offset on the y axis
     * @param maxLength the maximum length of the pane
     * @param maxHeight the maximum height of the pane
     */
    public abstract void display(@NotNull InventoryComponent inventoryComponent, int paneOffsetX, int paneOffsetY,
                                 int maxLength, int maxHeight);

    /**
     * Set the consumer that should be called whenever this pane is clicked in.
     *
     * @param onClick the consumer that gets called
     * @since 0.4.0
     */
    public void setOnClick(@Nullable Consumer<InventoryClickEvent> onClick) {
        this.onClick = onClick;
    }

    /**
     * Loads an item from an instance and an element
     *
     * @param instance the instance
     * @param element the element
     * @return the gui item
     */
    @NotNull
    @Contract(pure = true)
    public static GuiItem loadItem(@NotNull Object instance, @NotNull Element element) {
        String id = element.getAttribute("id");
        Material material = Objects.requireNonNull(Material.matchMaterial(id.toUpperCase(Locale.getDefault())));
        boolean hasAmount = element.hasAttribute("amount");
        boolean hasDamage = element.hasAttribute("damage");
        int amount = hasAmount ? Integer.parseInt(element.getAttribute("amount")) : 1;
        short damage = hasDamage ? Short.parseShort(element.getAttribute("damage")) : 0;

        //noinspection deprecation
        ItemStack itemStack = new ItemStack(material, amount, damage);

        List<Object> properties = new ArrayList<>();

        if (element.hasChildNodes()) {
            NodeList childNodes = element.getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node item = childNodes.item(i);

                if (item.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                Element elementItem = (Element) item;

                String nodeName = item.getNodeName();

                if (nodeName.equals("properties") || nodeName.equals("lore") || nodeName.equals("enchantments")) {
                    Element innerElement = (Element) item;
                    NodeList innerChildNodes = innerElement.getChildNodes();

                    for (int j = 0; j < innerChildNodes.getLength(); j++) {
                        Node innerNode = innerChildNodes.item(j);

                        if (innerNode.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        Element innerElementChild = (Element) innerNode;
                        ItemMeta itemMeta = Objects.requireNonNull(itemStack.getItemMeta());

                        switch (nodeName) {
                            case "properties":
                                if (!innerNode.getNodeName().equals("property"))
                                    continue;

                                String propertyType = innerElementChild.hasAttribute("type")
                                        ? innerElementChild.getAttribute("type")
                                        : "string";

                                properties.add(PROPERTY_MAPPINGS.get(propertyType).apply(innerElementChild
                                        .getTextContent()));
                                break;
                            case "lore":
                                if (!innerNode.getNodeName().equals("line"))
                                    continue;

                                boolean hasLore = itemMeta.hasLore();
                                List<String> lore = hasLore ? Objects.requireNonNull(itemMeta.getLore()) : new ArrayList<>();

                                lore.add(ChatColor.translateAlternateColorCodes('&', innerNode
                                        .getTextContent()));
                                itemMeta.setLore(lore);
                                itemStack.setItemMeta(itemMeta);
                                break;
                            case "enchantments":
                                if (!innerNode.getNodeName().equals("enchantment"))
                                    continue;

                                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(
                                    innerElementChild.getAttribute("id").toUpperCase(Locale.getDefault())
                                ));

                                if (enchantment == null) {
                                    throw new XMLLoadException("Enchantment cannot be found");
                                }

                                int level = Integer.parseInt(innerElementChild.getAttribute("level"));

                                itemMeta.addEnchant(enchantment, level, true);
                                itemStack.setItemMeta(itemMeta);
                                break;
                        }
                    }
                } else if (nodeName.equals("displayname")) {
                    ItemMeta itemMeta = Objects.requireNonNull(itemStack.getItemMeta());

                    itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item
                            .getTextContent()));

                    itemStack.setItemMeta(itemMeta);
                } else if (nodeName.equals("skull") && itemStack.getItemMeta() instanceof SkullMeta) {
                    SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

                    if (elementItem.hasAttribute("owner"))
                        //noinspection deprecation
                        skullMeta.setOwner(elementItem.getAttribute("owner"));
                    else if (elementItem.hasAttribute("id")) {
                        SkullUtil.setSkull(skullMeta, elementItem.getAttribute("id"));
                    }

                    itemStack.setItemMeta(skullMeta);
                }
            }
        }

        Consumer<InventoryClickEvent> action = null;

        if (element.hasAttribute("onClick")) {
            String methodName = element.getAttribute("onClick");
            for (Method method : instance.getClass().getMethods()) {
                if (!method.getName().equals(methodName))
                    continue;

                int parameterCount = method.getParameterCount();
                Class<?>[] parameterTypes = method.getParameterTypes();

                if (parameterCount == 0)
                    action = event -> {
                        try {
                            //because reflection with lambdas is stupid
                            method.setAccessible(true);
                            method.invoke(instance);
                        } catch (IllegalAccessException | InvocationTargetException exception) {
                            throw new XMLReflectionException(exception);
                        }
                    };
                else if (parameterTypes[0].isAssignableFrom(InventoryClickEvent.class)) {
                    if (parameterCount == 1)
                        action = event -> {
                            try {
                                //because reflection with lambdas is stupid
                                method.setAccessible(true);
                                method.invoke(instance, event);
                            } catch (IllegalAccessException | InvocationTargetException exception) {
                                throw new XMLReflectionException(exception);
                            }
                        };
                    else if (parameterCount == properties.size() + 1) {
                        boolean correct = true;

                        for (int i = 0; i < properties.size(); i++) {
                            Object attribute = properties.get(i);

                            if (!(parameterTypes[1 + i].isPrimitive() &&
                                    Primitives.unwrap(attribute.getClass()).isAssignableFrom(parameterTypes[1 + i])) &&
                                    !attribute.getClass().isAssignableFrom(parameterTypes[1 + i]))
                                correct = false;
                        }

                        if (correct) {
                            action = event -> {
                                try {
                                    //don't ask me why we need to do this, just roll with it (actually I do know why, but it's stupid)
                                    properties.add(0, event);

                                    //because reflection with lambdas is stupid
                                    method.setAccessible(true);
                                    method.invoke(instance, properties.toArray(new Object[0]));

                                    //since we'll append the event to the list next time again, we need to remove it here again
                                    properties.remove(0);
                                } catch (IllegalAccessException | InvocationTargetException exception) {
                                    throw new XMLReflectionException(exception);
                                }
                            };
                        }
                    }
                }

                break;
            }
        }

        GuiItem item = new GuiItem(itemStack, action);

        if (element.hasAttribute("field"))
            XMLUtil.loadFieldAttribute(instance, element, item);

        if (element.hasAttribute("populate")) {
            try {
                MethodUtils.invokeExactMethod(instance, "populate", item, GuiItem.class);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                throw new XMLLoadException(exception);
            }
        }
		
		item.setProperties(properties);

        return item;
    }

    public static void load(@NotNull Pane pane, @NotNull Object instance, @NotNull Element element) {
        if (element.hasAttribute("x")) {
            pane.setX(Integer.parseInt(element.getAttribute("x")));
        }

        if (element.hasAttribute("y")) {
            pane.setY(Integer.parseInt(element.getAttribute("y")));
        }

        if (element.hasAttribute("priority"))
            pane.setPriority(Priority.valueOf(element.getAttribute("priority")));

        if (element.hasAttribute("visible"))
            pane.setVisible(Boolean.parseBoolean(element.getAttribute("visible")));

        if (element.hasAttribute("field"))
            XMLUtil.loadFieldAttribute(instance, element, pane);

        if (element.hasAttribute("onClick"))
            pane.setOnClick(XMLUtil.loadOnEventAttribute(instance, element, InventoryClickEvent.class, "onClick"));

        if (element.hasAttribute("populate")) {
            String attribute = element.getAttribute("populate");
            for (Method method: instance.getClass().getMethods()) {
                if (!method.getName().equals(attribute))
                    continue;

                try {
                    method.setAccessible(true);
                    method.invoke(instance, pane);
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    throw new XMLLoadException(exception);
                }
            }
        }
    }

    /**
     * Finds a type of {@link GuiItem} from the provided collection of items based on the provided {@link ItemStack}.
     * The items will be compared using internal data. When the item does not have this data, this method will return
     * null. If the item does have such data, but its value cannot be found in the provided list, null is also returned.
     * This method will not mutate any of the provided arguments, nor any of the contents inside of the arguments. The
     * provided collection may be unmodifiable if preferred. This method will always return a type of {@link GuiItem}
     * that is in the provided collection - when the returned result is not null - such that an element E inside the
     * provided collection reference equals the returned type of {@link GuiItem}.
     *
     * @param items a collection of items in which will be searched
     * @param item the item for which an {@link GuiItem} should be found
     * @param <T> a type of GuiItem, which will be used in the provided collection and as return type
     * @return the found type of {@link GuiItem} or null if none was found
     * @since 0.5.14
     */
    @Nullable
    @Contract(pure = true)
    protected static <T extends GuiItem> T findMatchingItem(@NotNull Collection<T> items, @NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        UUID uuid = meta.getPersistentDataContainer().get(GuiItem.KEY_UUID, UUIDTagType.INSTANCE);
        if (uuid == null) {
            return null;
        }

        return items.stream()
                .filter(guiItem -> guiItem.getUUID().equals(uuid))
                .findAny().orElse(null);
    }
    
    /**
     * Calls the consumer (if it's not null) that was specified using {@link #setOnClick(Consumer)},
     * so the consumer that should be called whenever this pane is clicked in.
     * Catches and logs all exceptions the consumer might throw.
     *
     * @param event the event to handle
     * @since 0.6.0
     */
    protected void callOnClick(@NotNull InventoryClickEvent event) {
        if (onClick == null) {
            return;
        }
    
        try {
            onClick.accept(event);
        } catch (Throwable t) {
            Logger logger = JavaPlugin.getProvidingPlugin(getClass()).getLogger();
            logger.log(Level.SEVERE, "Exception while handling click event in inventory '"
                    + event.getView().getTitle() + "', slot=" + event.getSlot() + ", for "
                    + getClass().getSimpleName() + ", x=" + x + ", y=" + y + ", length=" + length + ", height=" + height, t);
        }
    }

    /**
     * Registers a property that can be used inside an XML file to add additional new properties.
     * The use of {@link Gui#registerProperty(String, Function)} is preferred over this method.
     *
     * @param attributeName the name of the property. This is the same name you'll be using to specify the property
     *                      type in the XML file.
     * @param function how the property should be processed. This converts the raw text input from the XML node value
     *                 into the correct object type.
     * @throws IllegalArgumentException when a property with this name is already registered.
     */
    public static void registerProperty(@NotNull String attributeName, @NotNull Function<String, Object> function) {
        if (PROPERTY_MAPPINGS.containsKey(attributeName)) {
            throw new IllegalArgumentException("property '" + attributeName + "' is already registered");
        }
    
        PROPERTY_MAPPINGS.put(attributeName, function);
    }

    static {
        PROPERTY_MAPPINGS.put("boolean", Boolean::parseBoolean);
        PROPERTY_MAPPINGS.put("byte", Byte::parseByte);
        PROPERTY_MAPPINGS.put("character", value -> value.charAt(0));
        PROPERTY_MAPPINGS.put("double", Double::parseDouble);
        PROPERTY_MAPPINGS.put("float", Float::parseFloat);
        PROPERTY_MAPPINGS.put("integer", Integer::parseInt);
        PROPERTY_MAPPINGS.put("long", Long::parseLong);
        PROPERTY_MAPPINGS.put("short", Short::parseShort);
        PROPERTY_MAPPINGS.put("string", value -> value);
    }
}